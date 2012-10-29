/**
 *  Copyright 2012 Wordnik, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.wordnik.swaggersocket.client

import scala.collection.JavaConversions._
import org.slf4j.{Logger, LoggerFactory}
import org.jfarcand.wcs.{TextListener, WebSocket}
import com.wordnik.swaggersocket.protocol.RequestMessage.Builder
import com.wordnik.swaggersocket.protocol.StatusMessage.Status
import java.util.concurrent.{ConcurrentHashMap, TimeoutException, TimeUnit, CountDownLatch}
import java.util.concurrent.atomic.AtomicInteger
import com.wordnik.swaggersocket.protocol.{Close, StatusMessage, Handshake, Request}

/**
 * A WebSocket connection supporting the SwaggerSocket protocol. As simple as:
 * <pre><blockquote>
 *    val swaggerSocket = SwaggerSocket().open("ws://127.0.0.1")
 *    swaggerSocket.send(new Request.Builder().path("/aResource").build, new SwaggerSocketListener() {

      override def message(r: Request, s: Response) {
      }
    }
 *
 *</blockquote></pre>
 */
object SwaggerSocket {

  def apply(timeoutInSeconds: Int): SwaggerSocket = {
    new SwaggerSocket("0", timeoutInSeconds, false, null, WebSocket())
  }

  def apply(): SwaggerSocket = {
    new SwaggerSocket("0", 30, false, null, WebSocket())
  }
}

case class SwaggerSocket(uniqueId : String, timeoutInSeconds: Int, isConnected: Boolean, activeRequests: ConcurrentHashMap[String, Request], w: WebSocket) {

  val logger: Logger = LoggerFactory.getLogger(classOf[SwaggerSocket])
  val deserializer = new SwaggerSocketDeserializer
  val serializer = new SwaggerSocketSerializer
  var path: String = "ws://localhost"
  var ws: WebSocket = null
  var identity : String = uniqueId

    /**
   * Open a WebSocket connection to a remote server.
   * @param a {@link Request}
   */
  def open(url: String): SwaggerSocket = {
    if (isConnected) throw new SwaggerSocketException(0, "Already Connected")
    open(new Request.Builder().path(url).build)
    this
  }

  /**
   * Open a WebSocket connection to a remote server.
   * @param a {@link Request}
   */
  def open(request: Request): SwaggerSocket = {
    if (isConnected) throw new SwaggerSocketException(0, "Already Connected")

    val l = new CountDownLatch(1)
    var e: Option[Throwable] = None
    val handshake: Handshake = new Handshake.Builder()
      .queryString(request.getQueryString)
      .headers(request.getHeaders)
      .format(request.getDataFormat)
      .method(request.getMethod)
      .path(request.getPath)
      .body(request.getMessageBody)
      .build
    val url = handshake.getPath + "?SwaggerSocket=1.0"

    try {
      ws = w.open(url)
      ws.listener(new TextListener {

        override def onOpen {
          ws.send(serializer.serializeHandshake(handshake))
        }

        override def onClose {
          ws.removeListener(this)
          l.countDown
        }

        override def onError(t: Throwable) {
          ws.removeListener(this)
          e = Some(t);
          l.countDown
        }

        override def onMessage(message: String) {
          try {
            val m = checkDelimiter(message)
            deserializer.deserializeStatus(m) match {
              case s: StatusMessage if (s.getStatus.getStatusCode < 400) =>
                identity = s.getIdentity
              case s: StatusMessage =>
                e = Some(new SwaggerSocketException(
                  s.getStatus.getStatusCode, s.getStatus.getReasonPhrase
                ))
              case _ => e = Some(
                new SwaggerSocketException(Status.NO_STATUS, "Null pointer")
              )
            }
          } catch {
            case ex: Throwable => logger.error("", ex)
          } finally {
            ws.removeListener(this)
            l.countDown
          }
        }
      })

      path = handshake.getPath
    } catch {
      case t: Exception => {
        logger.error("open", t)

        e = Some(new SwaggerSocketException(0, t.getMessage))
        l.countDown()
      }
    }

    if (!l.await(timeoutInSeconds, TimeUnit.SECONDS)) {
      throw new TimeoutException("Connect operation timed out after 30 seconds")
    }

    e.foreach(throw _)

    // Return a new instance with a unique identity
    new SwaggerSocket(identity, timeoutInSeconds, true, new ConcurrentHashMap[String, Request](), ws)

  }

  /**
   * CloseMessage the underlying WebSocket connection.
   */
  def close: SwaggerSocket = {
    val close: Close = new Close("Closed", identity);
    ws.send(serializer.serializeClose(close))
    ws.close
    this
  }

  /**
   * Send a request.
   * @param r a {@link Request}
   */
  def send(r: Request): SwaggerSocket = {
    send(Array(r), null)
    this
  }

  /**
   * Send an array of request. The Listener will be invoked as soon as response arrive.
   * @param r a {@link Request}
   * @param l a {@link SwaggerSocketListener} response's listener
   */
  def send(r: Request, l: SwaggerSocketListener): SwaggerSocket = {
    send(Array(r), l)
    this
  }

  /**
   * Add a {@link SwaggerSocketListener} response's listener.
   * @param l  a {@link SwaggerSocketListener}
   */
  def listener(l: SwaggerSocketListener): SwaggerSocket = {
    w.listener(new TextListener {

      override def onOpen {
        logger.trace("OnOpen " + this)
      }

      override def onClose {
        l.close
        logger.trace("onClose" + this)
      }

      override def onError(t: Throwable) {
        logger.error("", t)
        l.error(new SwaggerSocketException(500, t.getMessage))
      }

      /* will we get partial responses?  I think large messages can definitely be chunked */
      override def onMessage(message: String) {
        val m = checkDelimiter(message)
        if (message.startsWith("{\"status\"")) {
          val s: StatusMessage = deserializer.deserializeStatus(m)
          l.error(new SwaggerSocketException(s.getStatus.getStatusCode, s.getStatus.getReasonPhrase))
        } else {
          val responses = deserializer.deserializeResponse(m)
          responses.foreach(response => {
            try {
              val rq: Request = activeRequests(response.getUuid);
              l.message(rq, response)
            } catch {
              case ex: Throwable => {
                logger.error("", ex)
                l.error(new SwaggerSocketException(500, ex.getMessage))
              }
            } finally {
              activeRequests -= response.getUuid
            }
          })
          l.messages(responses)
        }
      }
    })
    this
  }

  /**
   * Send a an array of {@link Request}
   * @param r an array of {@link Request}
   */
  def send(r: Array[Request]): SwaggerSocket = {
    send(r, null)
    this
  }

  /**
   * Send a an array of {@link Request} and invoke the {@link  SwaggerSocketListener} as soon as response are received.
   * @param r an array of {@link Request}
   * @param l a {@link SwaggerSocketListener} response's listener
   */
  def send(r: Array[Request], l: SwaggerSocketListener): SwaggerSocket = {

    val requestMessage = new Builder().requests(r).identity(identity).build
    val callback: AtomicInteger = new AtomicInteger(r.size)

    //	will we have race conditions here?
    r.foreach(request => {
      activeRequests += request.getUuid -> request
    })

    if (l != null) {
      w.listener(new TextListener {

        override def onOpen {
          logger.trace("OnOpen " + this)
        }

        override def onClose {
          w.removeListener(this)
          l.close
        }

        override def onError(t: Throwable) {
          logger.error("", t);
          w.removeListener(this)
          l.error(new SwaggerSocketException(500, ""))
        }

        /* will we get partial responses?  I think large messages can definitely be chunked */
        override def onMessage(message: String) {
          val m = checkDelimiter(message)
          if (m.startsWith("{\"status\"")) {
            val s: StatusMessage = deserializer.deserializeStatus(m)
            l.error(new SwaggerSocketException(s.getStatus.getStatusCode, s.getStatus.getReasonPhrase))
          } else {
            val responses = deserializer.deserializeResponse(m)
            responses.foreach(response => {
              // Is this response for us
              val rq: Request = activeRequests(response.getUuid);
                try {
                  l.message(rq, response)
                } catch {
                  case ex: Throwable => l.error(new SwaggerSocketException(500, ex.getMessage))
                } finally {
                  activeRequests -= response.getUuid
                }
            })
            l.messages(responses)
          }
          if (callback.decrementAndGet() == 0) {
            w.removeListener(this)
          }
        }
      })
    }
    w.send(serializer.serializeRequests(requestMessage))
    this
  }

  def checkDelimiter(message: String) : String = {
    var m : Array[String] = message.split("<->")
    if (m.length == 1) {
      return m(0);
    } else {
      return m(1);
    }
  }

  override def toString: String = {
    path
  }

}

