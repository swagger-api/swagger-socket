package com.wordnik.swaggersocket.client

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.SerializationConfig;


object APIInvoker {
  val mapper = new ObjectMapper()
  mapper.getDeserializationConfig().set(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.getSerializationConfig().set(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false)
  mapper.configure(SerializationConfig.Feature.WRITE_NULL_PROPERTIES, false)
  mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false)

  def deserialize(response: String, className: Class[_]): AnyRef = {
    if (className.isAssignableFrom(classOf[String])) {
      response
    } else if (className.isAssignableFrom(classOf[java.lang.Integer])) {
      new java.lang.Integer(response)
    } else if (className.isAssignableFrom(classOf[java.lang.Boolean])) {
      new java.lang.Boolean(response)
    } else if (className.isAssignableFrom(classOf[java.lang.Long])) {
      new java.lang.Long(response)
    } else if (className.isAssignableFrom(classOf[java.lang.Double])) {
      new java.lang.Double(response)
    } else {
      mapper.readValue(response, className).asInstanceOf[AnyRef]
    }
  }

  def serialize(o: AnyRef): String = {
    mapper.writeValueAsString(o)
  }
}

abstract class APIInvoker {
  def invoke(resourceUrl: String, method: String, queryParams: Map[String, String], postData: AnyRef, headerParams: Map[String, String]): String
}