package com.wordnik.test.client

import com.wordnik.sample.api._
import com.wordnik.swagger.runtime.common._

import org.junit.runner.RunWith

import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import scala.collection.JavaConversions._

import scala.io._

@RunWith(classOf[JUnitRunner])
class ClientIT extends BaseTest with FlatSpec with ShouldMatchers {
  behavior of "swagger client library"

  it should "get a pet" in {
    APIInvoker.initialize(null, getTargetUrl, true)
	val pet = PetAPI.getPetById("1")
	assert(null != pet)
	assert(pet.id == 1)
  }
}