package com.wordnik.swaggersocket.samples

import org.atmosphere.nettosphere.Config
import com.wordnik.swaggersocket.server.{SwaggerSocketProtocolInterceptor}
import org.atmosphere.nettosphere.Nettosphere
import java.io.{InputStreamReader, BufferedReader}
import org.slf4j.{LoggerFactory, Logger}

/**
 * A NettoSphere Server that can be used without the needs of a Servlet Container.
 */
object NettoSphere {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[Nettosphere])

  def main(args: Array[String]): Unit = {

    var key = "0";
    if (args !=null && args.length > 0) {
      key = args(0)
    }

    var b: Config.Builder = new Config.Builder
    b.resource("./app")
      .initParam("com.wordnik.swagger.key", key)
      .initParam("com.sun.jersey.api.json.POJOMappingFeature", "true")
      .initParam("com.sun.jersey.config.property.packages", getClass.getPackage.getName)
      .interceptor(new SwaggerSocketProtocolInterceptor())
      .port(8080)
      .host("127.0.0.1")
      .build

    var s: Nettosphere = new Nettosphere.Builder().config(b.build).build
    s.start

    var a: String = ""
    logger.info("NettoSphere SwaggerSocket Server started on port {}", 8080)
    logger.info("Type quit to stop the server")
    var br: BufferedReader = new BufferedReader(new InputStreamReader(System.in))
    while (!((a == "quit"))) {
      a = br.readLine
    }
    System.exit(-1)
  }
}
