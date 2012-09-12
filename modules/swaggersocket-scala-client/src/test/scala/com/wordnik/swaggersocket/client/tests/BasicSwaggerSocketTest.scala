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
package com.wordnik.swaggersocket.client.tests

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import com.wordnik.swaggersocket.protocol.{Handshake, Request, Response}
import java.util.concurrent.{TimeUnit, CountDownLatch}
import com.wordnik.swaggersocket.client.{SwaggerSocketException, SwaggerSocketListener, SwaggerSocket}

@RunWith(classOf[JUnitRunner])
class BasicSwaggerSocketTest extends BaseTest with FlatSpec with ShouldMatchers {

  it should "simple open/request/response cycle in" in {
    var cd: CountDownLatch = new CountDownLatch(1)
    val ss : SwaggerSocket = SwaggerSocket()

    var req: Request = null
    var res: Response = null
    ss.open(new Request.Builder().path(getTargetUrl + "/test").build())
      .send(new Request.Builder()
      .path("/b")
      .method("POST")
      .body("Yo!")
      .format("application/json")
      .build(), new SwaggerSocketListener() {

      override def error(e: SwaggerSocketException) {
        cd.countDown()
      }

      override def message(r: Request, s: Response) {
        req = r
        res = s
        cd.countDown()
      }
    })

    cd.await(10, TimeUnit.SECONDS)
    ss.close

    assert(req != null)
    assert(res != null)

  }

  it should "See if request response uuid are matching" in {
    var cd: CountDownLatch = new CountDownLatch(1)
    val ss = SwaggerSocket()
    var uuidMatch = false;
    var request = new Request.Builder()
      .path("/b")
      .method("POST")
      .body("Yo!")
      .format("application/json")
      .build()

    var listener = new SwaggerSocketListener() {
      override def error(e: SwaggerSocketException) {
        cd.countDown()
      }

      override def message(r: Request, s: Response) {
        uuidMatch = r.getUuid == s.getUuid

        cd.countDown()
      }
    }

    ss.open(new Request.Builder().path(getTargetUrl + "/test").build())
      .send(request, listener)

    cd.await(10, TimeUnit.SECONDS)
    ss.close

    assert(uuidMatch)
  }

  it should "See if multiple request response uuid are matching" in {
    val open = new Request.Builder().path(getTargetUrl + "/test").build()
    var cd: CountDownLatch = new CountDownLatch(2)
    val ss = SwaggerSocket()
    var uuidMatch = false;
    var request = new Request.Builder()
      .path("/b")
      .method("POST")
      .body("Yo!")
      .format("application/json")
      .build()

    var request2 = new Request.Builder()
      .path("/a")
      .method("POST")
      .body("Yo!")
      .format("application/json")
      .build()

    var responseCount = 0

    var listener = new SwaggerSocketListener() {
      override def error(e: SwaggerSocketException) {
        cd.countDown()
      }

      override def message(r: Request, s: Response) {
        uuidMatch = r.getUuid == s.getUuid
        responseCount += 1

        cd.countDown()
      }
    }

    ss.open(open)
      .send(Array[Request](request, request2), listener)

    cd.await(10, TimeUnit.SECONDS)
    ss.close

    assert(uuidMatch)
    assert(responseCount == 2)
  }

  it should "See if multiple request response body are matching" in {
    val open = new Request.Builder().path(getTargetUrl + "/test").build()
    var cd: CountDownLatch = new CountDownLatch(2)
    val ss = SwaggerSocket()
    var bodyMatch = false;
    var request = new Request.Builder()
      .path("/b")
      .method("POST")
      .body("Yo!")
      .format("application/json")
      .build()

    var request2 = new Request.Builder()
      .path("/a")
      .method("POST")
      .body("Yo!")
      .format("application/json")
      .build()

    var responseCount = 0

    var listener = new SwaggerSocketListener() {
      override def error(e: SwaggerSocketException) {
        cd.countDown()
      }

      override def message(r: Request, s: Response) {
        bodyMatch = "Yo!" == s.getMessageBody
        responseCount += 1

        cd.countDown()
      }
    }

    ss.open(open)
      .send(Array[Request](request, request2), listener)

    cd.await(10, TimeUnit.SECONDS)
    ss.close

    assert(bodyMatch)
    assert(responseCount == 2)
  }

  it should "See if multiple request response body aren't matching" in {
    val open = new Request.Builder().path(getTargetUrl + "/test").build()
    var cd: CountDownLatch = new CountDownLatch(2)
    val ss = SwaggerSocket()
    var bodyMatch = true;
    var request = new Request.Builder()
      .path("/b")
      .method("POST")
      .body("Yo!")
      .format("application/json")
      .build()

    var request2 = new Request.Builder()
      .path("/a")
      .method("POST")
      .body("YoYo!")
      .format("application/json")
      .build()

    var responseCount = 0

    var listener = new SwaggerSocketListener() {
      override def error(e: SwaggerSocketException) {
        cd.countDown()
      }

      override def message(r: Request, s: Response) {
        bodyMatch = (r.getMessageBody == s.getMessageBody) && bodyMatch
        responseCount += 1

        cd.countDown()
      }
    }

    ss.open(open)
      .send(Array[Request](request, request2), listener)

    cd.await(10, TimeUnit.SECONDS)
    ss.close

    assert(bodyMatch)
    assert(responseCount == 2)
  }

  it should "See if multiple request response body are equals" in {
    val open = new Request.Builder().path(getTargetUrl + "/test").build()
    var cd: CountDownLatch = new CountDownLatch(2)
    val ss = SwaggerSocket()
    var bodyMatch = false;
    var request = new Request.Builder()
      .path("/b")
      .method("POST")
      .body("Yo!")
      .format("application/json")
      .build()

    var request2 = new Request.Builder()
      .path("/a")
      .method("POST")
      .body("YoYo!")
      .format("application/json")
      .build()

    var responseCount = 0

    var listener = new SwaggerSocketListener() {
      override def error(e: SwaggerSocketException) {
        cd.countDown()
      }

      override def message(r: Request, s: Response) {
        bodyMatch = r.getMessageBody == s.getMessageBody
        responseCount += 1

        cd.countDown()
      }
    }

    ss.open(open)
      .send(Array[Request](request, request2), listener)

    cd.await(10, TimeUnit.SECONDS)
    ss.close

    assert(bodyMatch)
    assert(responseCount == 2)
  }

  it should "Send invalid handhskake" in {
    val handshake = new Request.Builder().path("ws://127.0.0.1").build()

    try {
      SwaggerSocket().open(handshake)
      fail("Wrong exception")
    } catch {
      case e: SwaggerSocketException => assert(true)
      case e: Throwable => fail("Wrong exception")

    }
  }

  it should "request not found" in {
    var cd: CountDownLatch = new CountDownLatch(1)
    val ss = SwaggerSocket()

    var errorCode = 200
    ss.open(new Request.Builder().path(getTargetUrl + "/test").build())
      .send(new Request.Builder()
      .path("/baaa")
      .method("POST")
      .body("Yo!")
      .format("application/json")
      .build(), new SwaggerSocketListener() {

      override def error(e: SwaggerSocketException) {
        errorCode = e.getStatusCode
        cd.countDown()
      }

      override def message(r: Request, s: Response) {
        cd.countDown()
      }
    })

    cd.await(10, TimeUnit.SECONDS)
    ss.close

    assert(errorCode == 404)
  }

  it should "two concurrent requests to the wrong path fail" in {
    val open = new Request.Builder().path(getTargetUrl + "/test").build()
    var cd: CountDownLatch = new CountDownLatch(2)
    val ss = SwaggerSocket()
    var request = new Request.Builder()
      .path("/baaa")
      .method("POST")
      .body("Yo!")
      .format("application/json")
      .build()

    var request2 = new Request.Builder()
      .path("/abbb")
      .method("POST")
      .body("YoYo!")
      .format("application/json")
      .build()

    var errorCode: Array[Int] = new Array[Int](2)
    var i: Int = 0;
    var listener = new SwaggerSocketListener() {
      override def error(e: SwaggerSocketException) {
        errorCode(i) = e.getStatusCode
        i += 1
        cd.countDown()
      }

      override def message(r: Request, s: Response) {
        cd.countDown()
      }
    }

    ss.open(open)
      .send(Array[Request](request, request2), listener)

    cd.await(10, TimeUnit.SECONDS)
    ss.close

    errorCode.foreach(e => {
      assert(e == 404)
    })
  }
}

