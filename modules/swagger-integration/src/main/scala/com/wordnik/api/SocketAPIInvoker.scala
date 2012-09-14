/**
 *  Copyright 2011 Wordnik, Inc.
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
package com.wordnik.api


import scala.collection.JavaConversions._
import org.slf4j.LoggerFactory
import java.util.concurrent._
import com.wordnik.swaggersocket.protocol.{Header, QueryString, Response, Request}
import com.wordnik.swaggersocket.client.{SwaggerSocketException, SwaggerSocketListener, SwaggerSocket, APIInvoker}

class SocketAPIInvoker extends APIInvoker {
  val logger = LoggerFactory.getLogger(classOf[SocketAPIInvoker])
  var ss = SwaggerSocket()
  var host: String = null
  var latchs: ConcurrentLinkedQueue[CountDownLatch] = new ConcurrentLinkedQueue[CountDownLatch]
  var normalClose = false
  var cleaner : ExecutorService = Executors.newSingleThreadExecutor()
  val listener = new SwaggerSocketListener() {

    override def close {
      for (cd <- latchs) {
        cd.countDown
      }
      latchs.clear
      if (!normalClose) {
        logger.trace("Socket closed. Re-opening")
        try {
          ss = SwaggerSocket().open(new Request.Builder().path(host).build()).listener(this)
        } catch {
          case t: Throwable => logger.trace("Re-open exception", t)
        }
      }
    }


    override def error(e: SwaggerSocketException) {
      for (cd <- latchs) {
        cd.countDown
      }
      latchs.clear
      logger.error("Unexpected error {} ", e.getMessage, e)
    }

    override def message(r: Request, s: Response) {
      val cd: CountDownLatch = r.attachment match {
        case l: CountDownLatch => l
        case _ => throw new ClassCastException
      }

      r.attach(s.getMessageBody)
      cd.countDown

    }
  }

  def this(host: String) = {
    this ()
    this.host = host
    ss = ss.open(new Request.Builder().path(host).build()).listener(listener)
  }

  def invoke(resourcePath: String, method: String, queryParams: Map[String, String] = Map(), postData: AnyRef, headerParams: Map[String, String]) = {
    val cd: CountDownLatch = new CountDownLatch(1)

    val request = new Request.Builder()
      .path(resourcePath)
      .method(method.toUpperCase)
      .queryString(queryParams.map(p => new QueryString(p._1, p._2)).toList)
      .headers(headerParams.map(h => new Header(h._1, h._2)).toList)
      .format("JSON")
      .attach(cd)
      .body({
      postData match {
        case data: AnyRef => APIInvoker.serialize(data)
        case _ => null
      }
    }).build()

    ss.send(request)

    latchs.add(cd)
    // TODO: make it configurable
    try {
      if (!cd.await(4 * 60, TimeUnit.SECONDS)) {
        logger.error("No response after 120 seconds");
        ""
      } else {
        request.attachment.toString
      }
    } finally {

      cleaner.execute(new Runnable {
        override def run {
          latchs.remove(cd)
        }
      })
    }

  }

  def close() {
    cleaner.shutdownNow()
    normalClose = true
    ss.close
  }
}