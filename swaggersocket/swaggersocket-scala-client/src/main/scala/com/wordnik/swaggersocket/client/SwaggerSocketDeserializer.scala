package com.wordnik.swaggersocket.client

import com.wordnik.swaggersocket.server.{StatusMessage, ResponseMessage, Response}

class SwaggerSocketDeserializer {

  def deserializeResponse(s: String): java.util.List[Response] = {
     APIInvoker.deserialize(s,
      classOf[ResponseMessage]).asInstanceOf[ResponseMessage].getResponses
  }

  def deserializeStatus(s: String): StatusMessage = {
     APIInvoker.deserialize(s,
      classOf[StatusMessage]).asInstanceOf[StatusMessage]
  }
}