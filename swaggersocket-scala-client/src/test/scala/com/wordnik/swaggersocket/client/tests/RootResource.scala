package com.wordnik.swaggersocket.client.tests

import org.slf4j.LoggerFactory
import javax.ws.rs.{GET, POST, Produces, Path}

@Path("/")
@Produces(Array("application/json"))
class RootResource /* extend SwaggerSocket */ {

  val logger = LoggerFactory.getLogger(classOf[RootResource])

  @Path("/a")
  @POST
  def yo(m: String): String = {
    m
  }
  @Path("/b")
  @POST
  def yob(m: String): String = {
    m
  }

  @Path("/c")
  @POST
  def yoc(m: String): String = {
    "root::" + m
  }

  @Path("/pet.json/{yo}")
  @GET
  def yod(): String = {
    "root::zzzzzz!"
  }
}