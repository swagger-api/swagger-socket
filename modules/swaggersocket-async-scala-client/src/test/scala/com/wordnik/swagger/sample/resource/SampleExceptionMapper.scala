package com.wordnik.swagger.sample.resource

import com.wordnik.swagger.sample.exception.{ ApiException, BadRequestException, NotFoundException }
import com.wordnik.swagger.sample.model.ApiResponse

import javax.ws.rs.ext.{ ExceptionMapper, Provider }
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

@Provider
class ApplicationExceptionMapper extends ExceptionMapper[ApiException] {
  def toResponse(exception: ApiException): Response = {
    exception match {
      case e: NotFoundException =>
        Response.status(Status.NOT_FOUND).entity(new ApiResponse(ApiResponse.ERROR, e.getMessage())).build
      case e: BadRequestException =>
        Response.status(Status.BAD_REQUEST).entity(new ApiResponse(ApiResponse.ERROR, e.getMessage())).build
      case e: ApiException =>
        Response.status(Status.BAD_REQUEST).entity(new ApiResponse(ApiResponse.ERROR, e.getMessage())).build
      case _ =>
        Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ApiResponse(ApiResponse.ERROR, "a system error occured")).build
    }
  }
}

@Provider
class SampleExceptionMapper extends ExceptionMapper[Exception] {
  def toResponse(exception: Exception): Response = {
    exception match {
      case e: javax.ws.rs.WebApplicationException =>
        Response.status(e.getResponse.getStatus).entity(new ApiResponse(e.getResponse.getStatus, e.getMessage())).build
      case e: com.fasterxml.jackson.core.JsonParseException =>
        Response.status(400).entity(new ApiResponse(400, "bad input")).build
      case _ => {
        Response.status(500).entity(new ApiResponse(500, "something bad happened")).build
      }
    }
  }
}
