package com.wordnik.swaggersocket.client

import java.util.EnumSet
import javax.servlet.{Filter, DispatcherType}
import org.eclipse.jetty.servlet.{DefaultServlet, FilterHolder, ServletHolder, ServletContextHandler}
import javax.servlet.http.HttpServlet
import org.eclipse.jetty.server.Server

object JettyContainer {
  private val DefaultDispatcherTypes: EnumSet[DispatcherType] =
    EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC)
}

trait JettyContainer {
  import JettyContainer._

  def mount(klass: Class[_], path: String) = klass match {
    case servlet if classOf[HttpServlet].isAssignableFrom(servlet) =>
      addServlet(servlet.asInstanceOf[Class[_ <: HttpServlet]], path)
    case filter if classOf[Filter].isAssignableFrom(filter) =>
      addFilter(filter.asInstanceOf[Class[_ <: Filter]], path)
    case _ =>
      throw new IllegalArgumentException(klass + " is not assignable to either HttpServlet or Filter")
  }

  var resourceBasePath: String = "src/main/webapp"

  def mount(servlet: HttpServlet, path: String) = addServlet(servlet, path)

  def mount(app: Filter, path: String, dispatches: EnumSet[DispatcherType] = DefaultDispatcherTypes) =
    addFilter(app, path, dispatches)

  def addServlet(servlet: HttpServlet, path: String) = {
    val holder = new ServletHolder(servlet)
    servletContextHandler.addServlet(holder, path)
  }

  def addServlet(servlet: Class[_ <: HttpServlet], path: String) =
    servletContextHandler.addServlet(servlet, path)

  def addFilter(filter: Filter, path: String, dispatches: util.EnumSet[DispatcherType] = DefaultDispatcherTypes): FilterHolder = {
    val holder = new FilterHolder(filter)
    servletContextHandler.addFilter(holder, path, dispatches)
    holder
  }

  def addFilter(filter: Class[_ <: Filter], path: String): FilterHolder =
    addFilter(filter, path, DefaultDispatcherTypes)

  def addFilter(filter: Class[_ <: Filter], path: String, dispatches: util.EnumSet[DispatcherType]): FilterHolder =
    servletContextHandler.addFilter(filter, path, dispatches)

  // Add a default servlet.  If there is no underlying servlet, then
  // filters just return 404.
  addServlet(new DefaultServlet, "/")


  /**
   * Sets the port to listen on.  0 means listen on any available port.
   */
  def port: Int = 0

  /**
   * The port of the currently running Jetty.  May differ from port if port is 0.
   *
   * @return Some port if Jetty is currently listening, or None if it is not.
   */
  def localPort: Option[Int] = server.getConnectors.headOption map { _.getLocalPort }

  def contextPath = "/"

  lazy val server = new Server(port)

  lazy val servletContextHandler = {
    val handler = new ServletContextHandler(ServletContextHandler.SESSIONS)
    handler.setContextPath(contextPath)
    handler.setResourceBase(resourceBasePath)
    handler
  }

  def start(): Unit = {
    server.setHandler(servletContextHandler)
    server.start()
  }

  def stop(): Unit = server.stop()

  def baseUrl: String =
    server.getConnectors.headOption match {
      case Some(conn) =>
        val host = Option(conn.getHost) getOrElse "localhost"
        val port = conn.getLocalPort
        "http://%s:%d".format(host, port)
      case None =>
        sys.error("can't calculate base URL: no connector")
    }

  def websocketBaseUrl: String = baseUrl.replaceFirst("^http", "ws")
}
