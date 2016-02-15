package io.swagger.swaggersocket.samples

import org.atmosphere.nettosphere.Config
import io.swagger.swaggersocket.server.{SwaggerSocketProtocolInterceptor}
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
    if (args != null && args.length > 0) {
      key = args(0)
    }
    if (key == "0") {
      key = readLine("API-Key: ")
    }

    var p = getHttpPort()
    var b: Config.Builder = new Config.Builder
    b.resource("./app")
      .initParam("com.wordnik.swagger.key", key)
      .initParam("com.sun.jersey.api.json.POJOMappingFeature", "true")
      .initParam("com.sun.jersey.config.property.packages", getClass.getPackage.getName)
      .interceptor(new SwaggerSocketProtocolInterceptor())
      .port(p)
      .host("127.0.0.1")
      .build

    var s: Nettosphere = new Nettosphere.Builder().config(b.build).build
    s.start

    var a: String = ""
    logger.info("NettoSphere SwaggerSocket Server started on port {}", p)
    logger.info("Type quit to stop the server")
    while (!((a == "quit"))) {
      a = readLine
    }
    System.exit(-1)
  }

  private def getHttpPort(): Int = {
    var v = System.getProperty("nettosphere.port")
    if (v != null) {
      try {
        return Integer.parseInt(v);
      } catch {
        // ignore;
        case e:NumberFormatException => {}
      }
    }
    8080;
  }
}
