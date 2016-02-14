/**
 *  Copyright 2016 SmartBear Software
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
package io.swagger.swaggersocket.client.tests

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec

import io.swagger.swaggersocket.protocol.{Handshake, Request, Response}
import java.util.concurrent.{TimeUnit, CountDownLatch}
import io.swagger.swaggersocket.client.{SwaggerSocketException, SwaggerSocketListener, SwaggerSocket}

@RunWith(classOf[JUnitRunner])
class MultiResourceTest extends BaseTest {

  it should "See if multiple request response body match from different resource" in {
    val open = new Request.Builder().path(getTargetUrl + "/").build()
    var cd: CountDownLatch = new CountDownLatch(2)
    val ss = SwaggerSocket()
    var bodyMatch = false;

    // This will hit TestResource.scala
    var request = new Request.Builder()
      .path("/test/d")
      .method("POST")
      .body("Yo!")
      .format("application/json")
      .build()

    // This will hit RootResource.scala
    var request2 = new Request.Builder()
      .path("/c")
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
        bodyMatch = ("root::" + r.getMessageBody) == s.getMessageBody.toString

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
}

