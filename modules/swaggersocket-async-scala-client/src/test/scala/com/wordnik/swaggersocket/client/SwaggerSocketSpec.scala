package com.wordnik.swaggersocket.client

import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import akka.actor.{ExtendedActorSystem, ActorSystem}
import akka.testkit.TestKit
import com.typesafe.config.{ConfigFactory, Config}
import org.specs2.specification.{Step, Fragments}
import akka.dispatch.Await
import java.util.concurrent.TimeoutException
import akka.util.duration._
import com.wordnik.swaggersocket.server.SwaggerSocketServlet

object SwaggerSocketSpecification {
    private[this] val defaultConf = ConfigFactory.load()
  val testConf: Config = ConfigFactory.parseString("""
      akka {
        event-handlers = ["akka.testkit.TestEventListener"]
        loglevel = WARNING
        stdout-loglevel = WARNING
        actor {
          default-dispatcher {
            core-pool-size-factor = 2
            core-pool-size-min = 8
            core-pool-size-max = 8
            max-pool-size-factor = 2
            max-pool-size-min = 8
            max-pool-size-max = 8
          }
        }
      }
      """).withFallback(defaultConf.getConfig("test")).withFallback(defaultConf)

  def mapToConfig(cfg: Map[String, Any]): Config = {
    import scala.collection.JavaConverters._
    ConfigFactory.parseMap(cfg.asJava)
  }

  def getCallerName: String = getNonBaseCallerName("SwaggerSocketSpecification")

  def getNonBaseCallerName(name: String): String = {
    val s = Thread.currentThread.getStackTrace map (_.getClassName) drop 1 dropWhile (_ matches ".*%s.?$".format(name))
    s.head.replaceFirst(""".*\.""", "").replaceAll("[^a-zA-Z_0-9]", "_")
  }

}

abstract class SwaggerSocketSpecification(_system: ActorSystem) extends TestKit(_system) with Specification with NoTimeConversions with JettyContainer {
  def this(config: Config) = this(ActorSystem(SwaggerSocketSpecification.getCallerName, config.withFallback(SwaggerSocketSpecification.testConf)))

  def this(s: String) = this(ConfigFactory.parseString(s))

  def this(configMap: Map[String, _]) = this(SwaggerSocketSpecification.mapToConfig(configMap))

  def this() = this(ActorSystem(SwaggerSocketSpecification.getCallerName, SwaggerSocketSpecification.testConf))

  val a = new SwaggerSocketServlet()
  a.framework.addInitParameter("com.sun.jersey.config.property.packages", this.getClass.getPackage.getName)
  addServlet(a, "/*")

  override def map(fs: Fragments) = Step(start()) ^ super.map(fs) ^ Step(stop()) ^ Step(stopActors)

  private def stopActors = {
    import scala.util.control.Exception.ignoring
    ignoring(classOf[Throwable]) {
      system.shutdown()
      try Await.ready(system.asInstanceOf[ExtendedActorSystem].provider.terminationFuture, 5 seconds) catch {
        case _: TimeoutException ⇒ system.log.warning("Failed to stop [{}] within 5 seconds", system.name)
      }
    }
  }
}