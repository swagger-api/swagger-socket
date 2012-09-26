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
package com.wordnik.swaggersocket.tests

import org.slf4j.{ LoggerFactory, Logger }
import org.eclipse.jetty.server.nio.SelectChannelConnector
import java.net.ServerSocket
import org.eclipse.jetty.server.Server

import org.scalatest.{ FlatSpec, BeforeAndAfterAll }
import org.scalatest.matchers.ShouldMatchers
import org.eclipse.jetty.servlet.{ServletHolder, ServletContextHandler}
import com.wordnik.swaggersocket.server.SwaggerSocketServlet

class ApiInvokerTest extends Server with FlatSpec with BeforeAndAfterAll with ShouldMatchers {
  protected final val log: Logger = LoggerFactory.getLogger(classOf[ApiInvokerTest])
  protected var port1: Int = 0
  private var _connector: SelectChannelConnector = null
  protected var framework: SwaggerSocketServlet = null

  override def beforeAll(configMap: Map[String, Any]) = {
    setUpGlobal
  }

  override def afterAll(configMap: Map[String, Any]) = {
    tearDownGlobal
  }

  def setUpGlobal = {
    port1 = findFreePort
    _connector = new SelectChannelConnector
    _connector.setPort(port1)
    addConnector(_connector)

    var context: ServletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS)
    context.setContextPath("/")
    setHandler(context)
    val a = new SwaggerSocketServlet();
    a.framework.addInitParameter("com.sun.jersey.config.property.packages", this.getClass.getPackage.getName)
    context.addServlet(new ServletHolder(a), "/*")

    start
    log.info("Local HTTP server started successfully")
  }

  def tearDownGlobal = {
    stop
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

}