package com.wordnik.swaggersocket.client

import com.ning.http._
import client._
import akka.dispatch.{ExecutionContext, Future}
import akka.util.Duration
import akka.util.duration._
import java.net.URI
import org.json4s._


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

trait TransportClient {
  protected def baseUrl: String
  protected def clientConfig: AsyncHttpClientConfig
  implicit def execContext: ExecutionContext
  def submit(method: String, uri: String, params: Iterable[(String, Any)], headers: Iterable[(String, String)], body: String, timeout: Duration): Future[ClientResponse]
  def close()
}
