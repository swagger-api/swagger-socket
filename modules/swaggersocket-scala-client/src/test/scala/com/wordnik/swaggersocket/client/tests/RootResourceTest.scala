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
class RootResourceTest extends BaseTest with FlatSpec with ShouldMatchers {

  it should "simple open/request/response cycle in" in {
    var cd: CountDownLatch = new CountDownLatch(1)
    val ss = SwaggerSocket()

    var req: Request = null
    var res: Response = null
    ss.open(new Request.Builder().path(getTargetUrl + "/").build())
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

  it should "See if multiple request response body aren'tmatching" in {
    val open = new Request.Builder().path(getTargetUrl + "/").build()
    var cd: CountDownLatch = new CountDownLatch(2)
    val ss = SwaggerSocket()
    var bodyMatch = true;
    var request = new Request.Builder()
      .path("/b")
      .method("POST")
      .body("Yo!")
      .attach("Yo!")
      .format("application/json")
      .build()

    var request2 = new Request.Builder()
      .path("/a")
      .method("POST")
      .body("YoYo!")
      .attach("YoYo!")
      .format("application/json")
      .build()

    var responseCount = 0

    var listener = new SwaggerSocketListener() {
      override def error(e: SwaggerSocketException) {
        cd.countDown()
      }

      override def message(r: Request, s: Response) {
        bodyMatch = (r.attachment.toString == s.getMessageBody) && bodyMatch
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

  it should "simple wildcard test" in {
    var cd: CountDownLatch = new CountDownLatch(1)
    val ss = SwaggerSocket()

    var req: Request = null
    var res: Response = null
    ss.open(new Request.Builder().path(getTargetUrl + "/").build())
      .send(new Request.Builder()
      .path("/pet.json/1")
      .method("GET")
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
}

