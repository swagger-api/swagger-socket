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
package com.wordnik.swaggersocket.samples

import javax.ws.rs.{Produces, GET, POST, PUT, Path, PathParam}

@Path("/swaggersocket")
@Produces(Array("text/plain"))
class SwaggerSocketResource {

  @Path("/echo")
  @POST
  def echo(m: String): String = {
    // for testing a runtime exception handling
    if (m == "secret") {
        throw new RuntimeException("no secret")
    } else if (m.startsWith("sleep")) {
        // sleep n
        try {
            Thread.sleep(m.substring(6).trim.toInt * 1000)
            return "Good Morning"
        } catch {
            // ignore;
            case e:Exception => {}
        }
    }
    m
  }

  @Path("/ohce")
  @POST
  def ohce(m: String): String = {
    // for testing a runtime exception handling
    if (m == "secret") {
        throw new RuntimeException("no secret")
    }
    new StringBuilder(m.length).append(m).reverse.toString
  }

  @Path("/xbox/{word}")
  @GET
  @Produces(Array("text/xml"))
  def xbox(@PathParam("word") word: String): Box = {
    new Box("SwaggerSocket in Action", word)
  }

  @Path("/jbox/{word}")
  @GET
  @Produces(Array("application/json"))
  def jbox(@PathParam("word") word: String): Box = {
    new Box("SwaggerSocket in Action", word)
  }

  //for empty-entity test #27
  @Path("/put")
  @PUT
  def put(m: String): Unit = {
      System.out.println("Putting " + m);
  }

}