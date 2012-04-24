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

import org.slf4j.LoggerFactory
import javax.ws.rs.{GET, POST, Produces, Path}

@Path("/test")
@Produces(Array("application/json"))
class TestResource /* extend SwaggerSocket */ {

  val logger = LoggerFactory.getLogger(classOf[TestResource])

  @Path("/a")
  @POST
  def yo(m: String): String = {
    m
  }

  @Path("/b")
  @POST
  def swaggerSocket2(m: String): String = {
    m
  }

  @Path("/d")
  @POST
  def yoc(m: String): String = {
    "root::" + m
  }
}