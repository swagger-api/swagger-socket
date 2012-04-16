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
package com.wordnik.swaggersocket.samples

import java.io.InputStream
import javax.servlet.ServletContext
import javax.ws.rs.{PathParam, GET, Produces, Path}
import javax.ws.rs.core.{PathSegment, Context}

@Path("/")
class FileServiceResource {

  @Context
  private val sc: ServletContext = null

  @Path("/jquery/{id}")
  @Produces(Array("application/javascript"))
  @GET
  def getJQuery(@PathParam("id") ps: PathSegment): InputStream = {
    return sc.getResourceAsStream("/jquery/" + ps.getPath)
  }

  @GET
  @Produces(Array("text/html"))
  def getIndex: InputStream = {
    return sc.getResourceAsStream("/index.html")
  }

}