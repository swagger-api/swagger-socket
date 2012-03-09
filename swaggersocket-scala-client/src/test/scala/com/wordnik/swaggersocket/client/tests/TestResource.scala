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