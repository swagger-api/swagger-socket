package com.wordnik.swaggersocket.client
package tests

import akka.util.duration._
import akka.dispatch.Await

class PetApiClient extends SwaggerSocketSpecification {

  "A PetApiClient" should {
    "add a pet" in {
      val cfg = SwaggerConfig(baseUrl)
      val client = new SwaggerApiClient(cfg)
      val exp = Pet(11, Category(1, "Dogs"),"Dog 4", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available")
      val tst = client.pets.addPet(exp) onComplete {
        case Left(ex) => throw ex
        case Right(actual) => actual
      }
      Await.result(tst, 1 minute) must equal(exp)
      client.close()
    }

    "get a pet" in {
      val cfg = SwaggerConfig(baseUrl)
      val client = new SwaggerApiClient(cfg)
      val exp = Pet(1, Category(2, "Cats"), "Cat 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available")
      val tst = client.pets.getPetById(1) onComplete {
        case Left(ex) => throw ex
        case Right(actual) => actual
      }
      Await.result(tst, 1 minute) must equal(exp)
      client.close()
    }
  }
}