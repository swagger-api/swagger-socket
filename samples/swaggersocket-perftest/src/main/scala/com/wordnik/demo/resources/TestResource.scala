package com.wordnik.demo.resources

import com.wordnik.swagger.core._
import com.wordnik.swagger.jaxrs._

import com.wordnik.resource.util.ProfileEndpointTrait
import com.wordnik.util.perf.Profile

import org.atmosphere.jersey._
import org.atmosphere.annotation._
import org.atmosphere.cpr._

import javax.ws.rs._
import javax.ws.rs.core.Response

object Counter {
  @volatile
  var count: Double = 0
  def increment: Double = {
    count += 1
    count
  }
}

trait TestResource {
  @GET
  @Path("/simpleFetch")
  @ApiOperation(value = "Simple fetch method", notes = "", responseClass = "String")
  @ApiErrors(Array(
    new ApiError(code = 400, reason = "Bad request")))
  def getById() = Profile("/simpleFetch", {
    BroadcasterFactory.getDefault().lookup("perf-data", true).broadcast(Counter.increment)
    Response.ok.entity(new ApiResponse(200, "success")).build
  })

  @Suspend(scope = Suspend.SCOPE.APPLICATION)
  @GET
  @Path("/stream")
  def getStream = {
    new SuspendResponse.SuspendResponseBuilder[String]()
      .outputComments(true)
      .broadcaster(BroadcasterFactory.getDefault().lookup("perf-data", true))
      .scope(Suspend.SCOPE.APPLICATION)
      .build
  }
}

@Path("/api/test.json")
@Api(value = "/api/test", description = "Test resource")
@Produces(Array("application/json"))
class TestResourceJSON extends Help
  with ProfileEndpointTrait
  with TestResource

@Path("/api/test.xml")
@Api(value = "/api/test", description = "Test resource")
@Produces(Array("application/json"))
class TestResourceXML extends Help
  with ProfileEndpointTrait
  with TestResource
