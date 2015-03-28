/**
 *  Copyright 2015 Reverb Technologies, Inc.
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
package com.wordnik.swaggersocket.samples;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/swaggersocket")
@Produces({"text/plain"})
public class SwaggerSocketResource {

  @Path("/echo")
  @POST
  public String echo(String m) {
    // for testing a runtime exception handling
    if ("secret".equals(m)) {
        throw new RuntimeException("no secret");
    }
    return m;
  }

  @Path("/ohce")
  @POST
  public String ohce(String m) {
    // for testing a runtime exception handling
    if ("secret".equals(m)) {
        throw new RuntimeException("no secret");
    }
    return new StringBuilder(m.length()).append(m).reverse().toString();
  }

  @Path("/xbox/{word}")
  @GET
  @Produces({"application/xml"})
  public Box xbox(@PathParam("word") String word) {
    return new Box("SwaggerSocket in Action", word);
  }

  @Path("/jbox/{word}")
  @GET
  @Produces({"application/json"})
  public Box jbox(@PathParam("word") String word) {
    return new Box("SwaggerSocket in Action", word);
  }

  //for empty-entity test #27
  @Path("/put")
  @PUT
  public void put(String m) {
      System.out.println("Putting " + m);
  }
  
}
