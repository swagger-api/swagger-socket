/**
 *  Copyright 2012 Wordnik, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.wordnik.swaggersocket.client.tests

import org.slf4j.{ LoggerFactory, Logger }
import org.eclipse.jetty.server.AbstractConnector
import java.net.ServerSocket
import org.eclipse.jetty.server.Server

import org.scalatest.{ FlatSpec, BeforeAndAfterAll, ConfigMap }
import org.eclipse.jetty.servlet.{ServletHolder, ServletContextHandler}
import com.wordnik.swaggersocket.server.SwaggerSocketServlet

class BaseTest extends FlatSpec with BeforeAndAfterAll {
  protected final val log: Logger = LoggerFactory.getLogger(classOf[BaseTest])
  protected var port1: Int = 0
  private var _connector: AbstractConnector = null
  protected var framework: SwaggerSocketServlet = null
  protected var server: Server = new Server();

  override def beforeAll(configMap: ConfigMap) = {
    setUpGlobal
  }

  override def afterAll(configMap: ConfigMap) = {
    tearDownGlobal
  }

  def setUpGlobal = {
    port1 = findFreePort
    _connector = createConnector
    server.addConnector(_connector)

    var context: ServletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS)
    context.setContextPath("/")
    server.setHandler(context)
    val a = new SwaggerSocketServlet();
    a.framework.addInitParameter("com.sun.jersey.config.property.packages", this.getClass.getPackage.getName)
    context.addServlet(new ServletHolder(a), "/*")

    server.start
    log.info("Local HTTP server started successfully")
  }

  def createConnector: AbstractConnector = {
    var usejetty9: Boolean = false
    // probe
    try {
      Class.forName("org.eclipse.jetty.server.ServerConnector")
      usejetty9 = true
    } catch {
      case e: Exception =>
        try {
          Class.forName("org.eclipse.jetty.server.nio.SelectChannelConnector")
        } catch {
          case e: Exception => fail("No jetty! This should not happen, though")
        }
    }

    if (usejetty9) {
      // jetty9
      var clz = Class.forName("org.eclipse.jetty.server.ServerConnector")
      var c = clz.getConstructor(classOf[Server]).newInstance(server).asInstanceOf[AbstractConnector]
      clz.getMethod("setPort", classOf[Int]).invoke(c, new Integer(port1))
      c
    } else {
      // jetty8
      var clz = Class.forName("org.eclipse.jetty.server.nio.SelectChannelConnector")
      var c = clz.newInstance().asInstanceOf[AbstractConnector]
      clz.getMethod("setPort", classOf[Int]).invoke(c, new Integer(port1))
      c
    }
  }

  def tearDownGlobal = {
    server.stop
  }

  protected def findFreePort: Int = {
    var socket: ServerSocket = null
    try {
      socket = new ServerSocket(0)
      return socket.getLocalPort
    } finally {
      if (socket != null) {
        socket.close
      }
    }
  }

  protected def getTargetUrl: String = {
    "ws://127.0.0.1:" + port1;
  }
}