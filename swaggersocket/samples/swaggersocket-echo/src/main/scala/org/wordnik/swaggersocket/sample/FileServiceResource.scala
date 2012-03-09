package org.wordnik.swaggersocket.sample

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