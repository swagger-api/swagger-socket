package com.wordnik.swagger.sample.resource

import com.wordnik.swagger.annotations._
import com.wordnik.swagger.jaxrs._

import com.wordnik.swagger.sample.model.User
import com.wordnik.swagger.sample.data.UserData
import com.wordnik.swagger.sample.exception.NotFoundException

import javax.ws.rs.core.Response
import javax.ws.rs._
import com.wordnik.swagger.core.util.RestResourceUtil
import scala.collection.JavaConverters._

trait UserResource extends RestResourceUtil {
  @POST
  @ApiOperation(value = "Create user", responseClass = "com.wordnik.swagger.sample.model.User", notes = "This can only be done by the logged in user.")
  def createUser(
    @ApiParam(value = "Created user object", required = true) user: User) = {
    val newUser = UserData.addUser(user)
    Response.ok.entity(newUser).build
  }

  @POST
  @Path("/createWithArray")
  @ApiOperation(value = "Creates list of users with given input array", responseClass = "com.wordnik.swagger.sample.model.User", multiValueResponse = true)
  def createUsersWithArrayInput(@ApiParam(value = "List of user object", required = true) users: Array[User]): Response = {
    val newUsers = for (user <- users) yield UserData.addUser(user)
    Response.ok.entity(newUsers.toList).build
  }

  @POST
  @Path("/createWithList")
  @ApiOperation(value = "Creates list of users with given list input", responseClass = "com.wordnik.swagger.sample.model.User", multiValueResponse = true)
  def createUsersWithListInput(@ApiParam(value = "List of user object", required = true) users: java.util.List[User]): Response = {
    val newUsers = for (user <- users.asScala) yield UserData.addUser(user)
    Response.ok.entity(newUsers).build
  }

  @PUT
  @Path("/{username}")
  @ApiOperation(value = "Updated user", notes = "This can only be done by the logged in user.", responseClass = "com.wordnik.swagger.sample.model.User")
  @ApiErrors(Array(
    new ApiError(code = 400, reason = "Invalid username supplied"),
    new ApiError(code = 404, reason = "User not found")))
  def updateUser(
    @ApiParam(value = "name that need to be deleted", required = true)@PathParam("username") username: String,
    @ApiParam(value = "Updated user object", required = true) user: User) = {
    val updated = UserData.addUser(user)
    Response.ok.entity(updated).build
  }

  @DELETE
  @Path("/{username}")
  @ApiOperation(value = "Delete user", notes = "This can only be done by the logged in user.")
  @ApiErrors(Array(
    new ApiError(code = 400, reason = "Invalid username supplied"),
    new ApiError(code = 404, reason = "User not found")))
  def deleteUser(
    @ApiParam(value = "The name that needs to be deleted", required = true)@PathParam("username") username: String) = {
    UserData.removeUser(username)
    Response.ok.entity(Map.empty).build
  }

  @GET
  @Path("/{username}")
  @ApiOperation(value = "Get user by user name", responseClass = "com.wordnik.swagger.sample.model.User")
  @ApiErrors(Array(
    new ApiError(code = 400, reason = "Invalid username supplied"),
    new ApiError(code = 404, reason = "User not found")))
  def getUserByName(
    @ApiParam(value = "The name that needs to be fetched. Use user1 for testing. ", required = true)@PathParam("username") username: String) = {
    var user = UserData.findUserByName(username)
    if (null != user) {
      Response.ok.entity(user).build
    } else {
      throw new NotFoundException(404, "User not found")
    }
  }

  @GET
  @Path("/login")
  @ApiOperation(value = "Logs user into the system", responseClass = "java.lang.String")
  @ApiErrors(Array(
    new ApiError(code = 400, reason = "Invalid username and password combination")))
  def loginUser(
    @ApiParam(value = "The user name for login", required = true)@QueryParam("username") username: String,
    @ApiParam(value = "The password for login in clear text", required = true)@QueryParam("password") password: String) = {
    Response.ok.entity("logged in user session:" + System.currentTimeMillis()).build
  }

  @GET
  @Path("/logout")
  @ApiOperation(value = "Logs out current logged in user session")
  def logoutUser() = {
    Response.ok.entity("").build
  }
}

@Path("/user.json")
@Api(value = "/user", description = "Operations about user")
@Produces(Array("application/json"))
class UserResourceJSON extends UserResource

@Path("/user.xml")
@Api(value = "/user", description = "Operations about user")
@Produces(Array("application/xml"))
class UserResourceXML extends UserResource