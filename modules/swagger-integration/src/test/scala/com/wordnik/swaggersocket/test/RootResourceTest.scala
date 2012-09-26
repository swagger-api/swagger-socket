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
package com.wordnik.swaggersocket.tests

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import com.wordnik.swaggersocket.protocol.{Handshake, Request, Response}
import java.util.concurrent.{TimeUnit, CountDownLatch}
import com.wordnik.swaggersocket.client.{SwaggerSocketException, SwaggerSocketListener, SwaggerSocket}
import com.wordnik.api.ApiInvoker
import collection.mutable.HashMap

@RunWith(classOf[JUnitRunner])
class RootResourceTest extends ApiInvokerTest with FlatSpec with ShouldMatchers {
  var apiInvoker = ApiInvoker

  it should "simple open/request/response cycle in" in {
    val queryParams = new HashMap[String, String]
    val headerParams = new HashMap[String, String]

    apiInvoker.invokeApi("rootresource", port1.toString, "/b", "POST", queryParams.toMap, "yo!", headerParams.toMap);
  }
}

