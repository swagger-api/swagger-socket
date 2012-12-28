package com.wordnik.swaggersocket.client
package tests

import org.json4s.DefaultFormats
import akka.util.Duration
import akka.util.duration._
import akka.dispatch.Await

class RestClientTest extends SwaggerSocketSpecification {

//  case class SwaggerConfig(
//    baseUrl: String,
//    userAgent: String = RestClient.DefaultUserAgent,
//    dataFormat: SwaggerConfig.DataFormat = DataFormat.Json(DefaultFormats),
//    idleTimeout: Duration = 5 minutes,
//    maxMessageSize: Int = 8912,
//    enableCompression: Boolean = true,
//    followRedirects: Boolean = true,
//    identity: String = "0")

  "A RestClient" should {
    "add a pet" in {
      val cfg = SwaggerConfig(baseUrl)
      println("config: " + cfg)
      val client = new SwaggerApiClient(cfg)
      val exp = Pet(11, Category(1, "Dogs"),"Dog 4", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available")
      val tst = client.pets.addPet(exp) onComplete {
        case Left(ex) =>
          ex.printStackTrace()
          throw ex
        case Right(actual) =>
          println("result: " + actual)
          actual
      }
      Await.result(tst, 1 minute) must equal(exp)
    }
  }
}