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

  def toPathValue(value: String): String = {
    value match {
      case v: String => encode(value)
      case _ => ""
    }
  }

  def encode(value: String): String = {
    try {
      return java.net.URLEncoder.encode(value, "utf-8").replaceAll("\\+", "%20");
    } catch  {
      case e:Exception => throw new RuntimeException(e.getMessage());
    }
  }
}

abstract class APIInvoker {
  def invoke(resourceUrl: String, method: String, queryParams: Map[String, String], postData: AnyRef, headerParams: Map[String, String]): String
}