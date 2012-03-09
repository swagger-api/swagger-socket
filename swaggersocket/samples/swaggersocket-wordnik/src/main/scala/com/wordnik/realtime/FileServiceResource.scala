package com.wordnik.realtime

import java.io.InputStream
import javax.servlet.ServletContext
import javax.ws.rs.{PathParam, GET, Produces, Path}
import javax.ws.rs.core.{PathSegment, Context}

@Path("/")
@Produces(Array("text/html"))
class FileServiceResource {

  @Path("/jquery/{id}")
  @Produces(Array("application/javascript"))
  @GET def getJQuery(@PathParam("id") ps: PathSegment): InputStream = {
    return sc.getResourceAsStream("/jquery/" + ps.getPath)
  }

  @GET def getIndex: InputStream = {
    return sc.getResourceAsStream("/index.html")
  }

  @Path("w.png")
  @Produces(Array("image/png"))
  @GET def getImage: InputStream = {
    return sc.getResourceAsStream("/w.png")
  }

  @Context private val sc: ServletContext = null
}