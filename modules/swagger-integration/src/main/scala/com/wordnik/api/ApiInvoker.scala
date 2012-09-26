/**
 * Copyright 2011 Wordnik, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wordnik.api


import scala.collection.JavaConversions._
import org.slf4j.LoggerFactory
import java.util.concurrent._
import com.wordnik.swaggersocket.protocol.{Header, QueryString, Response, Request}
import com.wordnik.swaggersocket.client.{SwaggerSocketException, SwaggerSocketListener, SwaggerSocket, APIInvoker}
import org.codehaus.jackson.map.{SerializationConfig, ObjectMapper}
import org.codehaus.jackson.map.DeserializationConfig.Feature
import java.net.URLEncoder
import collection.mutable.HashMap

object JsonUtil {
  def getJsonMapper = {
    val mapper = new ObjectMapper()
    mapper.getDeserializationConfig().set(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.getSerializationConfig().set(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false)
    mapper.configure(SerializationConfig.Feature.WRITE_NULL_PROPERTIES, false)
    mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false)

    mapper
  }
}

object ApiInvoker {
  val defaultHeaders: HashMap[String, String] = HashMap()
  val logger = LoggerFactory.getLogger(ApiInvoker.getClass)
  var ss = SwaggerSocket()
  var host: String = "127.0.0.1"
  var latchs: ConcurrentLinkedQueue[CountDownLatch] = new ConcurrentLinkedQueue[CountDownLatch]
  var normalClose = false
  var cleaner: ExecutorService = Executors.newSingleThreadExecutor()
  val listener = new SwaggerSocketListener() {

    override def close {
      for (cd <- latchs) {
        cd.countDown
      }
      latchs.clear
      if (!normalClose) {
        logger.trace("Socket closed. Re-opening")
//        try {
//          ss = SwaggerSocket().open(new Request.Builder().path(host).build()).listener(this)
//        } catch {
//          case t: Throwable => logger.trace("Re-open exception", t)
//        }
      }
    }

    def toPathValue(value: String): String = {
      value match {
        case v: String => encode(value)
        case _ => ""
      }
    }

    def encode(value: String): String = {
      try {
        return java.net.URLEncoder.encode(value, "utf-8").replaceAll("\\+", "%20");
      } catch {
        case e: Exception => throw new RuntimeException(e.getMessage());
      }
    }

    override def error(e: SwaggerSocketException) {
      for (cd <- latchs) {
        cd.countDown
      }
      latchs.clear
      logger.error("Unexpected error {} {}", e.getStatusCode, e.getReasonPhrase)
      logger.error("", e)
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

  def escapeString(value: String): String = {
    URLEncoder.encode(value, "utf-8").replaceAll("\\+", "%20")
  }

  def deserialize(response: String, containerType: String, className: Class[_]) = {
    if (className.isAssignableFrom(classOf[String])) {
      response
    } else if (className.isAssignableFrom(classOf[java.lang.Integer])) {
      new java.lang.Integer(response)
    } else if (className.isAssignableFrom(classOf[java.lang.Boolean])) {
      new java.lang.Boolean(response)
    } else if (className.isAssignableFrom(classOf[java.lang.Long])) {
      new java.lang.Long(response)
    } else if (className.isAssignableFrom(classOf[java.lang.Double])) {
      new java.lang.Double(response)
    } else {
      JsonUtil.getJsonMapper.readValue(response, className).asInstanceOf[AnyRef]
    }
  }

  def serialize(o: AnyRef): String = {
    JsonUtil.getJsonMapper.writeValueAsString(o)
  }

  def invokeApi(serviceName: String, portPath: String, path: String, method: String, queryParams: Map[String, String], body: AnyRef, headerParams: Map[String, String]) = {
    // TODO: Needs to have an implementation of the SwaggerLocator
//    val host = SwaggerLocator.endpointFor(serviceName)
//    host match {
//      case None => throw new ApiException(503, "no host for " + serviceName + " available")
//      case _ =>
//    }
    val fq = "ws://" + host + ":" + portPath

    ss = ss.open(new Request.Builder().path(fq).build()).listener(listener)
    val cd: CountDownLatch = new CountDownLatch(1)

    headerParams.map(p => new Header(p._1, p._2))
    defaultHeaders.map(p => {
      headerParams.contains(p._1) match {
        case true => // override default with supplied header
        case false => if (p._2 != null) new Header(p._1, p._2)
      }
    })

    val request = new Request.Builder()
      .path(path)
      .method(method.toUpperCase)
      .queryString(queryParams.map(p => new QueryString(p._1, p._2)).toList)
      .headers(headerParams.map(h => new Header(h._1, h._2)).toList)
      .format("application/json")
      .attach(cd)
      .body({
      body match {
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

class ApiException extends Exception {
  var code = 0

  def this(code: Int, msg: String) = {
    this()
  }
}