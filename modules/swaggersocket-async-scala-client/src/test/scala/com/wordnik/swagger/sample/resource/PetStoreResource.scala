package com.wordnik.swagger.sample.resource

import com.wordnik.swagger.core._
import com.wordnik.swagger.annotations._

import com.wordnik.swagger.core.util.RestResourceUtil
import com.wordnik.swagger.jaxrs._
import com.wordnik.swagger.sample.model.Order
import com.wordnik.swagger.sample.data.StoreData
import com.wordnik.swagger.sample.exception.NotFoundException

import javax.ws.rs.core.Response
import javax.ws.rs._

trait PetStoreResource extends RestResourceUtil {
  @GET
  @Path("/order/{orderId}")
  @ApiOperation(value = "Find purchase order by ID", notes = "For valid response try integer IDs with value <= 5. " +
    "Anything above 5 or nonintegers will generate API errors", responseClass = "com.wordnik.swagger.sample.model.Order")
  @ApiErrors(Array(
    new ApiError(code = 400, reason = "Invalid ID supplied"),
    new ApiError(code = 404, reason = "Order not found")))
  def getOrderById(
      @ApiParam(value="ID of pet that needs to be fetched",required=true)@PathParam("orderId") orderId: String) = {
      var order = StoreData.findOrderById(getLong(0,10000, 0, orderId))
      if (null != order){
        Response.ok.entity(order).build
      }else{
        throw new NotFoundException(404, "Order not found")
      }
  }

  @POST
  @Path("/order")
  @ApiOperation(value = "Place an order for a pet", responseClass = "void")
  @ApiErrors(Array(
    new ApiError(code = 400, reason = "Invalid order")))
  def placeOrder(
      @ApiParam(value="order placed for purchasing the pet",required=true)order: Order) = {
      StoreData.placeOrder(order)
      Response.ok.build
  }

  @DELETE
  @Path("/order/{orderId}")
  @ApiOperation(value = "Delete purchase order by ID", notes = "For valid response try integer IDs with value < 1000. " +
    "Anything above 1000 or nonintegers will generate API errors")
  @ApiErrors(Array(
    new ApiError(code = 400, reason = "Invalid ID supplied"),
    new ApiError(code = 404, reason = "Order not found")))
  def deleteOrder(
      @ApiParam(value="ID of the order that needs to be deleted",required=true)@PathParam("orderId") orderId: String) = {
      StoreData.deleteOrder(getLong(0, 10000, 0, orderId))
      Response.ok.build
  }
}

@Path("/store.json")
@Api(value="/store" , description = "Operations about store")
@Produces(Array("application/json"))
class PetStoreResourceJSON extends Help
  with PetStoreResource

@Path("/store.xml")
@Api(value="/store", description = "Operations about store")
@Produces(Array("application/xml"))
class PetStoreResourceXML extends Help
  with PetStoreResource