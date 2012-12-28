package com.wordnik.swagger.sample.util

import javax.ws.rs.Produces

import javax.ws.rs.core.MediaType
import javax.ws.rs.ext.Provider

import com.wordnik.swagger.core.util.JsonUtil

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider
import com.fasterxml.jackson.databind._

import com.fasterxml.jackson.module.scala.DefaultScalaModule

import com.fasterxml.jackson.core.JsonGenerator.Feature
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@Provider
@Produces(Array(MediaType.APPLICATION_JSON))
class JacksonJsonProvider extends JacksonJaxbJsonProvider {
  val mapper = new ObjectMapper()
  mapper.registerModule(new DefaultScalaModule())
  mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
  mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
  mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
  mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  super.setMapper(mapper)
}