package com.wordnik.swaggersocket.client

import com.ning.http._
import client._
import client.{ Cookie => AhcCookie }
import collection.JavaConverters._
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import io.{Codec, Source}
import java.nio.charset.Charset
import java.io.File
import java.net.URI
import rl.MapQueryString
import akka.dispatch.{Promise, ExecutionContext, Future}
import akka.util.Duration
import akka.util.duration._
import org.json4s.JsonAST.JValue
import org.json4s.jackson.JsonMethods


object RestClient {
  val DefaultUserAgent = "SwaggerClient/1.0"

  private implicit def stringWithExt(s: String) = new {
    def isBlank = s == null || s.trim.isEmpty
    def nonBlank = !isBlank


    def blankOption = if (isBlank) None else Option(s)
  }

  case class CookieOptions(
          domain  : String  = "",
          path    : String  = "",
          maxAge  : Int     = -1,
          secure  : Boolean = false,
          comment : String  = "",
          httpOnly: Boolean = false,
          encoding: String  = "UTF-8")

  trait HttpCookie {
    implicit def cookieOptions: CookieOptions
    def name: String
    def value: String

  }

  case class RequestCookie(name: String, value: String, cookieOptions: CookieOptions = CookieOptions()) extends HttpCookie
  case class Cookie(name: String, value: String)(implicit val cookieOptions: CookieOptions = CookieOptions()) extends HttpCookie {

    private def ensureDotDomain = if (!cookieOptions.domain.startsWith("."))
      "." + cookieOptions.domain
    else
      cookieOptions.domain

    def toCookieString = {
      val sb = new StringBuffer
      sb append name append "="
      sb append value

      if(cookieOptions.domain.nonBlank)
        sb.append("; Domain=").append(ensureDotDomain.toLowerCase(Locale.ENGLISH))

      val pth = cookieOptions.path
      if(pth.nonBlank) sb append "; Path=" append (if(!pth.startsWith("/")) {
        "/" + pth
      } else { pth })

      if(cookieOptions.comment.nonBlank) sb append ("; Comment=") append cookieOptions.comment

      if(cookieOptions.maxAge > -1) sb append "; Max-Age=" append cookieOptions.maxAge

      if (cookieOptions.secure) sb append "; Secure"
      if (cookieOptions.httpOnly) sb append "; HttpOnly"
      sb.toString
    }

  }


  class CookieJar(private val reqCookies: Map[String, RequestCookie])  {
    private val cookies = new ConcurrentHashMap[String, HttpCookie].asScala ++ reqCookies

    def get(key: String) = cookies.get(key) filter (_.cookieOptions.maxAge != 0) map (_.value)

    def apply(key: String) = get(key) getOrElse (throw new Exception("No cookie could be found for the specified key [%s]" format key))

    def update(name: String, value: String)(implicit cookieOptions: CookieOptions=CookieOptions()) = {
      cookies += name -> Cookie(name, value)(cookieOptions)
    }

    def set(name: String, value: String)(implicit cookieOptions: CookieOptions=CookieOptions()) = {
      this.update(name, value)(cookieOptions)
    }

    def delete(name: String)(implicit cookieOptions: CookieOptions = CookieOptions(maxAge = 0)) {
      this.update(name, "")(cookieOptions.copy(maxAge = 0))
    }

    def +=(keyValuePair: (String, String))(implicit cookieOptions: CookieOptions = CookieOptions()) = {
      this.update(keyValuePair._1, keyValuePair._2)(cookieOptions)
    }

    def -=(key: String)(implicit cookieOptions: CookieOptions = CookieOptions(maxAge = 0)) {
      delete(key)(cookieOptions)
    }

    def size =  cookies.size

    def foreach[U](fn: (HttpCookie) => U) = cookies foreach { case (_, v) => fn(v) }

    private[client] def responseCookies = cookies.values collect { case c: Cookie => c }

    override def toString: String = cookies.toString()
  }


  class RestClientResponse(response: Response) extends ClientResponse {
    val cookies = (response.getCookies.asScala map { cookie =>
      val cko = CookieOptions(cookie.getDomain, cookie.getPath, cookie.getMaxAge)
      cookie.getName -> Cookie(cookie.getName, cookie.getValue)(cko)
    }).toMap

    val headers = (response.getHeaders.keySet().asScala map { k => k -> response.getHeaders(k).asScala.toSeq}).toMap

    val status = ResponseStatus(response.getStatusCode, response.getStatusText)

    val contentType = response.getContentType

    val inputStream = response.getResponseBodyAsStream

    val uri = response.getUri

    private[this] var _body: JValue = null

    def body = {
      if (_body == null) _body = JsonMethods.parse(inputStream, useBigDecimalForDouble = true)
      _body
    }

    private[this] def nioCharset = charset map Charset.forName getOrElse Codec.UTF8
    def mediaType: Option[String] = headers.get("Content-Type") flatMap { _.headOption }

    def charset: Option[String] =
      for {
        ct <- mediaType
        charset <- ct.split(";").drop(1).headOption
      } yield charset.toUpperCase.replace("CHARSET=", "").trim
  }


}


trait ClientResponse {
  def cookies: Map[String, RestClient.Cookie]
  def headers: Map[String, Seq[String]]
  def status: ResponseStatus
  def contentType: String
  def mediaType: Option[String]
  def charset: Option[String]
  def uri: URI
  def statusCode: Int = status.code
  def statusText: String = status.line
  def body: JValue

}

object StringHttpMethod {
  val GET = "GET"
  val POST = "POST"
  val DELETE = "DELETE"
  val PUT = "PUT"
  val CONNECT = "CONNECT"
  val HEAD = "HEAD"
  val OPTIONS = "OPTIONS"
  val PATCH = "PATCH"
  val TRACE = "TRACE"
}

class RestClient(config: SwaggerConfig) extends TransportClient {

  protected val baseUrl: String = config.baseUrl
  protected val clientConfig: AsyncHttpClientConfig = (new AsyncHttpClientConfig.Builder()
    setUserAgent config.userAgent
    setRequestTimeoutInMs config.idleTimeout.toMillis.toInt
    setCompressionEnabled config.enableCompression               // enable content-compression
    setAllowPoolingConnection true                               // enable http keep-alive
    setFollowRedirects config.followRedirects).build()

  import RestClient._
  import StringHttpMethod._
  implicit val execContext = ExecutionContext.fromExecutorService(clientConfig.executorService())

  private val mimes = new Mimes {
    protected def warn(message: String) = System.err.println("[WARN] " + message)
  }

  private val cookies = new CookieJar(Map.empty)

  private[this] val underlying = new AsyncHttpClient(clientConfig) {
    def preparePatch(uri: String): AsyncHttpClient#BoundRequestBuilder = requestBuilder(PATCH, uri)
    def prepareTrace(uri: String): AsyncHttpClient#BoundRequestBuilder = requestBuilder(TRACE, uri)
  }

  private def requestFactory(method: String): String ⇒ AsyncHttpClient#BoundRequestBuilder = {
    method.toUpperCase(Locale.ENGLISH) match {
      case `GET`     ⇒ underlying.prepareGet _
      case `POST`    ⇒ underlying.preparePost _
      case `PUT`     ⇒ underlying.preparePut _
      case `DELETE`  ⇒ underlying.prepareDelete _
      case `HEAD`    ⇒ underlying.prepareHead _
      case `OPTIONS` ⇒ underlying.prepareOptions _
      case `CONNECT` ⇒ underlying.prepareConnect _
      case `PATCH`   ⇒ underlying.preparePatch _
      case `TRACE`   ⇒ underlying.prepareTrace _
    }
  }

  private def addParameters(method: String, params: Iterable[(String, String)], isMultipart: Boolean = false, charset: Charset = Codec.UTF8)(req: AsyncHttpClient#BoundRequestBuilder) = {
    method.toUpperCase(Locale.ENGLISH) match {
      case `GET` | `DELETE` | `HEAD` | `OPTIONS` ⇒ params foreach { case (k, v) ⇒ req addQueryParameter (k, v) }
      case `PUT` | `POST`   | `PATCH`            ⇒ {
        if (!isMultipart)
          params foreach { case (k, v) ⇒ req addParameter (k, v) }
        else {
          params foreach { case (k, v) => req addBodyPart new StringPart(k, v, charset.name)}
        }
      }
      case _                                     ⇒ // we don't care, carry on
    }
    req
  }

  private def addHeaders(headers: Iterable[(String, String)])(req: AsyncHttpClient#BoundRequestBuilder) = {
    headers foreach { case (k, v) => req.addHeader(k, v) }
    req
  }

  private val allowsBody = Vector(PUT, POST, PATCH)


  def submit(method: String, uri: String, params: Iterable[(String, Any)], headers: Iterable[(String, String)], body: String, timeout: Duration = 5.seconds): Future[RestClientResponse] = {
    val base = URI.create(baseUrl).normalize()
    val u = URI.create(uri).normalize()
    val files = params collect {
      case (k, v: File) => k -> v
    }
    val realParams = params collect {
      case (k, v: String) => k -> v
      case (k, null) => k -> ""
      case (k, v) => k -> v.toString
    }
    val isMultipart = {
      allowsBody.contains(method.toUpperCase(Locale.ENGLISH)) && {
        val ct = (defaultWriteContentType(files) ++ headers)("Content-Type")
        ct.toLowerCase(Locale.ENGLISH).startsWith("multipart/form-data")
      }
    }

    val reqUri = if (u.isAbsolute) u else {
      // There is no constructor on java.net.URI that will not encode the path
      // except for the one where you pass in a uri as string so we're concatenating ourselves
//      val uu = new URI(base.getScheme, base.getUserInfo, base.getHost, base.getPort, base.getRawPath + u.getRawPath, u.getRawQuery, u.getRawFragment)
      val b = "%s://%s:%d".format(base.getScheme, base.getHost, base.getPort)
      val p = base.getRawPath + u.getRawPath.blankOption.getOrElse("/")
      val q = u.getRawQuery.blankOption.map("?"+_).getOrElse("")
      val f = u.getRawFragment.blankOption.map("#"+_).getOrElse("")
      URI.create(b+p+q+f)
    }
    val req = (requestFactory(method)
      andThen (addHeaders(headers) _)
      andThen (addParameters(method, realParams, isMultipart) _))(reqUri.toASCIIString)
    val prc = new PerRequestConfig()
    prc.setRequestTimeoutInMs(timeout.toMillis.toInt)
    req.setPerRequestConfig(prc)
    if (isMultipart) {
      files foreach { case (nm, file) =>
        req.addBodyPart(new FilePart(nm, file, mimes(file), FileCharset(file).name))
      }
    }
    if (cookies.size > 0) {
      cookies foreach { cookie =>
        val ahcCookie = new AhcCookie(
          cookie.cookieOptions.domain,
          cookie.name,
          cookie.value,
          cookie.cookieOptions.path,
          cookie.cookieOptions.maxAge,
          cookie.cookieOptions.secure)
        req.addCookie(ahcCookie)
      }
    }
    u.getQuery.blankOption foreach { uu =>
      MapQueryString.parseString(uu) foreach { case (k, v) => v foreach { req.addQueryParameter(k, _) } }
    }
    if (allowsBody.contains(method.toUpperCase(Locale.ENGLISH)) && body.nonBlank) req.setBody(body)

    val promise = Promise[RestClientResponse]()
    req.execute(async(promise))
    promise
  }

  private[this] def defaultWriteContentType(files: Iterable[(String, File)]) = {
    val value = if (files.nonEmpty) "multipart/form-data" else config.dataFormat.contentType
    Map("Content-Type" -> value)
  }

  private[this] def async(promise: Promise[RestClientResponse]) = new AsyncCompletionHandler[Future[ClientResponse]] {


    override def onThrowable(t: Throwable) {
      promise.complete(Left(t))
    }

    def onCompleted(response: Response) = {
      promise.complete(Right(new RestClientResponse(response)))
    }
  }


  def close() = underlying.closeAsynchronously()
}
