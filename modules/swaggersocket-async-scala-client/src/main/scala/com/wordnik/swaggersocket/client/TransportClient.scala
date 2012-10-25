package com.wordnik.swaggersocket.client

import com.ning.http._
import client._
import akka.dispatch.Future
import akka.util.Duration
import akka.util.duration._

trait TransportClient {
  protected def baseUrl: String
  protected def clientConfig: AsyncHttpClientConfig
  def submit(method: String, uri: String, params: Iterable[(String, Any)], headers: Iterable[(String, String)], body: String, timeout: Duration): Future[ClientResponse]
  def close()
}
