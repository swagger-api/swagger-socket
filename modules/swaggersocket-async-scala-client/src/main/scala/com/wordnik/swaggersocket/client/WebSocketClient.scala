package com.wordnik.swaggersocket.client

import akka.util.Duration
import akka.dispatch.{Promise, ExecutionContext, Future}
import com.ning.http.client.{Cookie, AsyncHttpClient, AsyncHttpClientConfig}
import org.json4s._
import jackson.JsonMethods
import java.util.UUID
import com.ning.http.client.websocket._
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import collection.mutable
import collection.JavaConverters._
import org.slf4j.LoggerFactory
import java.net.{HttpCookie, URI}
import java.nio.charset.Charset
import io.Codec
import java.util.concurrent.atomic.AtomicBoolean
import RestClient.CookieOptions

object WebSocketClient {
  sealed trait SwaggerSocketMessage
  case class Close(identity: String, reason: String) extends SwaggerSocketMessage

  case class Heartbeat(identity: String, heartbeat: String) extends SwaggerSocketMessage

  sealed trait Param {
    def name: String
    def value: String
  }
  case class QueryParam(name: String, value: String) extends Param
  case class HeaderParam(name: String, value: String) extends Param
  case class FormParam(name: String, value: String) extends Param

  sealed trait ProtocolMessage {
    def headers: List[HeaderParam]
    def queryString: List[QueryParam]
    def path: String
    def method: String
    def uuid: String
    def messageBody: JValue
    def dataFormat: String
  }
  case class Handshake(
    protocolName: String,
    protocolVersion: String,
    path: String,
    method: String,
    dataFormat: String,
    headers: List[HeaderParam] = Nil,
    queryString: List[QueryParam] = Nil,
    uuid: String = "0",
    messageBody: JValue = JNothing) extends ProtocolMessage

  case class HandshakeEnvelope(handshake: Handshake) extends SwaggerSocketMessage

  case class Request(
    path: String,
    method: String,
    dataFormat: String,
    headers: List[HeaderParam] = Nil,
    queryString: List[QueryParam] = Nil,
    uuid: String = UUID.randomUUID.toString,
    messageBody: JValue = JNothing,
    attachment: Option[Any]= None) extends ProtocolMessage

  case class RequestEnvelope(identity: String, requests: List[Request]) extends SwaggerSocketMessage

  case class Response(
    status: Int,
    reasonPhrase: String,
    path: String,
    method: String,
    dataFormat: String,
    headers: List[HeaderParam] = Nil,
    queryString: List[QueryParam] = Nil,
    uuid: String = UUID.randomUUID.toString,
    messageBody: JValue = JNothing,
    attachment: Option[Any] = None) extends ProtocolMessage

  case class ResponseEnvelope(identity: String, responses: List[Response]) extends SwaggerSocketMessage

  case class ActiveRequest(uuid: String, request: Request, responseFuture: Promise[Response])

  case class Status(statusCode: Int, reasonPhrase: String)
  case class StatusMessage(status: Status, identity: String) extends SwaggerSocketMessage

  object SwaggerSocketResponse {
    def apply(config: SwaggerConfig)(resp: Response): ClientResponse = new SwaggerSocketResponse(resp, config)
  }
  class SwaggerSocketResponse(resp: Response, config: SwaggerConfig) extends ClientResponse {
    val uri: URI = URI.create(resp.path)

    val body = resp.messageBody

    val cookies: Map[String, RestClient.Cookie] = {
      val allCks = mutable.Map.empty[String, RestClient.Cookie]
      resp.headers.filter(_.name equalsIgnoreCase "set-cookie") foreach { hp =>
        val cookie = HttpCookie.parse(hp.value)
        val cks = mutable.Map.empty[String, RestClient.Cookie]
        cookie.asScala foreach { c =>
          cks += c.getName -> RestClient.Cookie(c.getName, c.getValue)(CookieOptions(c.getDomain, c.getPath, c.getMaxAge.toInt, c.getSecure, c.getComment, c.isHttpOnly, "utf-8"))
        }
        allCks ++= cks
      }
      allCks.toMap
    }

    val status: ResponseStatus = ResponseStatus(resp.status, resp.reasonPhrase)


    val headers: Map[String, Seq[String]] =  {
      val mp = Map.empty[String, List[String]].withDefaultValue(List.empty[String])
      resp.headers.foldLeft(mp) { (acc, hp) => acc.updated(hp.name, hp.value :: acc(hp.name)) }
    }
    val contentType: String = {
      val df = resp.dataFormat
      if (df != null && df.trim.nonEmpty) df
      else headers.get("Content-Type").flatMap(_.headOption).getOrElse(config.dataFormat.contentType)
    }

    private[this] def nioCharset = charset map Charset.forName getOrElse Codec.UTF8
    def mediaType: Option[String] = {
      val df = resp.dataFormat
      if (df != null && df.trim.nonEmpty)
        Some(df)
      else
        headers.get("Content-Type") flatMap { _.headOption.flatMap(_.split(";").headOption) }
    }

    def charset: Option[String] =
      for {
        ct <- headers.get("Content-Type").flatMap(_.headOption)
        charset <- ct.split(";").drop(1).headOption
      } yield charset.toUpperCase.replace("CHARSET=", "").trim
  }
}

class WebSocketClient(config: SwaggerConfig) extends TransportClient {

  import WebSocketClient._
  import StringHttpMethod._

  private[this] val logger = LoggerFactory.getLogger(getClass)

  protected val baseUrl: String = config.baseUrl
  protected val clientConfig: AsyncHttpClientConfig = (new AsyncHttpClientConfig.Builder()
    setUserAgent config.userAgent
    setRequestTimeoutInMs config.idleTimeout.toMillis.toInt
    setCompressionEnabled config.enableCompression               // enable content-compression
    setAllowPoolingConnection true                               // enable http keep-alive
    setFollowRedirects config.followRedirects).build()

  private[this] val underlying = new AsyncHttpClient(clientConfig) {
    def preparePatch(uri: String): AsyncHttpClient#BoundRequestBuilder = requestBuilder(PATCH, uri)
    def prepareTrace(uri: String): AsyncHttpClient#BoundRequestBuilder = requestBuilder(TRACE, uri)
  }
  private[this] val isConnected: AtomicBoolean = new AtomicBoolean(false)
  private[this] val activeRequests: mutable.ConcurrentMap[String, ActiveRequest] = new ConcurrentHashMap[String, ActiveRequest].asScala

  private[this] val wsHandler = (new WebSocketUpgradeHandler.Builder
    setMaxTextSize config.maxMessageSize
    setMaxByteSize config.maxMessageSize
    addWebSocketListener new WebSocketTextListener with WebSocketCloseCodeReasonListener {
      def onClose(websocket: WebSocket) {
        if (logger.isTraceEnabled) logger.trace("onClose")
      }
      def onError(t: Throwable) {
        if (logger.isErrorEnabled) logger.error(t.getMessage, t)
      }
      def onOpen(websocket: WebSocket) {
        if (logger.isTraceEnabled) logger.trace("onOpen")
      }

      def onFragment(fragment: String, last: Boolean) {}

      def onMessage(message: String) {
        val isConn = isConnected.get()
        if (!isConn) {
          if (message.startsWith("{\"status\"")) {
            isConnected.set(true)
          }
        } else {
          if (!message.startsWith("{\"status\"")) {
            val env = config.dataFormat.deserialize[ResponseEnvelope](message)
            env.responses foreach { resp =>
              activeRequests.get(resp.uuid) foreach { ar =>
                ar.responseFuture.complete(Right(resp))
              }
            }
          }
        }
      }

      def onClose(websocket: WebSocket, code: Int, reason: String) {}
    }
    setProtocol null).build()

  implicit val execContext = ExecutionContext.fromExecutorService(clientConfig.executorService())

  private[this] val webSocket = underlying.prepareGet(baseUrl.replaceFirst("^http", "ws")).execute(wsHandler).get(config.idleTimeout.toMillis, TimeUnit.MILLISECONDS)

  def close() { underlying.closeAsynchronously() }

  def submit(method: String, uri: String, params: Iterable[(String, Any)], headers: Iterable[(String, String)], body: String, timeout: Duration): Future[ClientResponse] = {
    val req = Request(
      uri,
      method,
      config.dataFormat.contentType,
      headers map ((HeaderParam.apply _).tupled) toList,
      params map (kv => QueryParam(kv._1, kv._2.toString)) toList,
      messageBody = JsonMethods.parse(body))
    val promise = Promise[Response]
    activeRequests(req.uuid) = ActiveRequest(req.uuid, req, promise)
    webSocket addWebSocketListener new WebSocketTextListener {
      def onClose(websocket: WebSocket) {
        webSocket.removeWebSocketListener(this)
      }
      def onError(t: Throwable) {
        webSocket.removeWebSocketListener(this)
        promise.complete(Left(t))
      }
      def onOpen(websocket: WebSocket) {

      }

      def onFragment(fragment: String, last: Boolean) {}

      def onMessage(message: String) {

      }
    }
    webSocket.sendTextMessage(config.dataFormat.serialize(RequestEnvelope(config.identity, List(req))))
    promise map SwaggerSocketResponse(config)
  }
}
