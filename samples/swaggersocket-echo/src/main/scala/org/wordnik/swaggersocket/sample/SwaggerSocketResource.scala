package org.wordnik.swaggersocket.sample

import org.slf4j.LoggerFactory
import javax.ws.rs.{Produces, POST, Path}

@Path("/swaggersocket")
@Produces(Array("application/json"))
class SwaggerSocketResource /* extend SwaggerSocket */ {

  val logger = LoggerFactory.getLogger(classOf[SwaggerSocketResource])

  @Path("/echo")
  @POST
  def swaggerSocket2(m: String): String = {
    m
  }

}