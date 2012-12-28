package com.wordnik.swaggersocket.client

import com.ning.http._
import client._
import client.{ Cookie => AhcCookie }
import collection.JavaConverters._
import java.util.{TimeZone, Date, Locale}
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
import java.text.SimpleDateFormat


object RestClient {
  val DefaultUserAgent = "SwaggerClient/1.0"

  private implicit def stringWithExt(s: String) = new {
    def isBlank = s == null || s.trim.isEmpty
    def nonBlank = !isBlank
    def blankOption = if (isBlank) None else Option(s)
  }

  case class CookieOptions( domain  : String  = "",
                            path    : String  = "",
                            maxAge  : Int     = -1,
                            secure  : Boolean = false,
                            comment : String  = "",
                            httpOnly: Boolean = false,
                            version : Int = 0,
                            encoding: String  = "UTF-8")

  trait HttpCookie {
    implicit def cookieOptions: CookieOptions
    def name: String
    def value: String

  }

  case class RequestCookie(name: String, value: String, cookieOptions: CookieOptions = CookieOptions()) extends HttpCookie
  object DateUtil {
    @volatile private[this] var _currentTimeMillis: Option[Long] = None
    def currentTimeMillis = _currentTimeMillis getOrElse System.currentTimeMillis
    def currentTimeMillis_=(ct: Long) = _currentTimeMillis = Some(ct)
    def freezeTime() = _currentTimeMillis = Some(System.currentTimeMillis())
    def unfreezeTime() = _currentTimeMillis = None
    def formatDate(date: Date, format: String, timeZone: TimeZone = TimeZone.getTimeZone("GMT")) = {
      val df = new SimpleDateFormat(format)
      df.setTimeZone(timeZone)
      df.format(date)
    }
  }


  case class Cookie(name: String, value: String)(implicit val cookieOptions: CookieOptions = CookieOptions()) extends HttpCookie {

    private def ensureDotDomain =
      (if (!cookieOptions.domain.startsWith(".")) "." + cookieOptions.domain else cookieOptions.domain).toLowerCase(Locale.ENGLISH)

    def toCookieString = {
      val sb = new StringBuffer
      sb append name append "="
      sb append value

      if(cookieOptions.domain.nonBlank && cookieOptions.domain != "localhost")
        sb.append("; Domain=").append(ensureDotDomain)

      val pth = cookieOptions.path
      if(pth.nonBlank) sb append "; Path=" append (if(!pth.startsWith("/")) {
        "/" + pth
      } else { pth })

      if(cookieOptions.comment.nonBlank) sb append ("; Comment=") append cookieOptions.comment

      appendMaxAge(sb, cookieOptions.maxAge, cookieOptions.version)

      if (cookieOptions.secure) sb append "; Secure"
      if (cookieOptions.httpOnly) sb append "; HttpOnly"
      sb.toString
    }
    private[this] def appendMaxAge(sb: StringBuffer, maxAge: Int, version: Int) = {
      val dateInMillis = maxAge match {
         case a if a < 0 => None // we don't do anything for max-age when it's < 0 then it becomes a session cookie
         case 0 => Some(0L) // Set the date to the min date for the system
         case a => Some(DateUtil.currentTimeMillis + a * 1000)
      }

      // This used to be Max-Age but IE is not always very happy with that
      // see: http://mrcoles.com/blog/cookies-max-age-vs-expires/
      // see Q1: http://blogs.msdn.com/b/ieinternals/archive/2009/08/20/wininet-ie-cookie-internals-faq.aspx
      val bOpt = dateInMillis map (ms => appendExpires(sb, new Date(ms)))
      val agedOpt = if (version > 0) bOpt map (_.append("; Max-Age=").append(maxAge)) else bOpt
      agedOpt getOrElse sb
    }

    private[this] def appendExpires(sb: StringBuffer, expires: Date) =
      sb append  "; Expires=" append formatExpires(expires)

    private[this] def formatExpires(date: Date) = DateUtil.formatDate(date, "EEE, dd MMM yyyy HH:mm:ss zzz")

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

  private[this] val mimes = new Mimes {
    protected def warn(message: String) = System.err.println("[WARN] " + message)
  }

  private[this] val cookies = new CookieJar(Map.empty)

  private[this] val underlying = new AsyncHttpClient(clientConfig) {
    def preparePatch(uri: String): AsyncHttpClient#BoundRequestBuilder = requestBuilder(PATCH, uri)
    def prepareTrace(uri: String): AsyncHttpClient#BoundRequestBuilder = requestBuilder(TRACE, uri)
  }

  private[this] def requestFactory(method: String): String ⇒ AsyncHttpClient#BoundRequestBuilder = {
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

  private[this] def addTimeout(timeout: Duration)(req: AsyncHttpClient#BoundRequestBuilder) = {
    if (timeout.isFinite()) {
      val prc = new PerRequestConfig()
      prc.setRequestTimeoutInMs(timeout.toMillis.toInt)
      req.setPerRequestConfig(prc)
    }
    req
  }

  private[this] def addParameters(method: String, params: Iterable[(String, String)], isMultipart: Boolean = false, charset: Charset = Codec.UTF8)(req: AsyncHttpClient#BoundRequestBuilder) = {
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

  private[this] def addHeaders(headers: Iterable[(String, String)])(req: AsyncHttpClient#BoundRequestBuilder) = {
    headers foreach { case (k, v) => req.addHeader(k, v) }
    req
  }

  private[this] def addFiles(files: Iterable[(String, File)], isMultipart: Boolean)(req: AsyncHttpClient#BoundRequestBuilder) = {
    if (isMultipart) {
      files foreach { case (nm, file) =>
        req.addBodyPart(new FilePart(nm, file, mimes(file), FileCharset(file).name))
      }
    }
    req
  }

  private[this] def addCookies(req: AsyncHttpClient#BoundRequestBuilder) = {
    cookies foreach { cookie =>
      val ahcCookie = new AhcCookie(
        cookie.cookieOptions.domain,
        cookie.name,
        cookie.value,
        cookie.cookieOptions.path,
        cookie.cookieOptions.maxAge,
        cookie.cookieOptions.secure,
        cookie.cookieOptions.version)
      req.addCookie(ahcCookie)
    }
    req
  }

  private[this] def addQuery(u: URI)(req: AsyncHttpClient#BoundRequestBuilder) = {
    u.getQuery.blankOption foreach { uu =>
      MapQueryString.parseString(uu) foreach { case (k, v) => v foreach { req.addQueryParameter(k, _) } }
    }
    req
  }

  private[this] val allowsBody = Vector(PUT, POST, PATCH)


  private[this] def addBody(method: String, body: String)(req: AsyncHttpClient#BoundRequestBuilder) = {
    if (allowsBody.contains(method.toUpperCase(Locale.ENGLISH)) && body.nonBlank) req.setBody(body)
    req
  }


  private[this] def requestFiles(params: Iterable[(String, Any)]) = params collect { case (k, v: File) => k -> v }
  private[this] def paramsFrom(params: Iterable[(String, Any)]) = params collect {
    case (k, v: String) => k -> v
    case (k, null) => k -> ""
    case (k, v) => k -> v.toString
  }
  private[this] def isMultipartRequest(method: String, headers: Iterable[(String, String)], files: Iterable[(String, File)]) = {
    allowsBody.contains(method.toUpperCase(Locale.ENGLISH)) && {
      val ct = (defaultWriteContentType(files) ++ headers)("Content-Type")
      ct.toLowerCase(Locale.ENGLISH).startsWith("multipart/form-data")
    }
  }

  private[this] def requestUri(base: URI, u: URI) = if (u.isAbsolute) u else {
    // There is no constructor on java.net.URI that will not encode the path
    // except for the one where you pass in a uri as string so we're concatenating ourselves
    val b = "%s://%s:%d".format(base.getScheme, base.getHost, base.getPort)
    val p = base.getRawPath + u.getRawPath.blankOption.getOrElse("/")
    val q = u.getRawQuery.blankOption.map("?"+_).getOrElse("")
    val f = u.getRawFragment.blankOption.map("#"+_).getOrElse("")
    URI.create(b+p+q+f)
  }

  def submit(method: String, uri: String, params: Iterable[(String, Any)], headers: Iterable[(String, String)], body: String, timeout: Duration = 5.seconds): Future[RestClientResponse] = {
    val u = URI.create(uri).normalize()
    val files = requestFiles(params)
    val isMultipart = isMultipartRequest(method, headers, files)

    (requestFactory(method)
      andThen addTimeout(timeout)
      andThen addHeaders(headers)
      andThen addCookies
      andThen addParameters(method, paramsFrom(params), isMultipart)
      andThen addQuery(u)
      andThen addBody(method, body)
      andThen addFiles(files, isMultipart)
      andThen executeRequest)(requestUri(URI.create(baseUrl).normalize(), u).toASCIIString)
  }

  private[this] def executeRequest(req: AsyncHttpClient#BoundRequestBuilder) = {
    val promise = Promise[RestClientResponse]()
    req.execute(new AsyncCompletionHandler[Future[ClientResponse]] {
      override def onThrowable(t: Throwable) { promise.complete(Left(t)) }
      def onCompleted(response: Response) = { promise.complete(Right(new RestClientResponse(response))) }
    })
    promise
  }

  private[this] def defaultWriteContentType(files: Iterable[(String, File)]) = {
    val value = if (files.nonEmpty) "multipart/form-data" else config.dataFormat.contentType
    Map("Content-Type" -> value)
  }

  def close() = underlying.closeAsynchronously()
}
