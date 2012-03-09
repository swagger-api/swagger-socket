package com.wordnik.swaggersocket.client

import org.codehaus.jackson.map.ObjectMapper
import com.wordnik.swaggersocket.server.{RequestMessage, HandshakeMessage, Handshake}

class SwaggerSocketSerializer {

  val mapper:ObjectMapper  = new ObjectMapper()

  def serializeRequests(requests : RequestMessage): String = {
    mapper.writeValueAsString(requests)
  }

  def serializeHandshake(h : Handshake) : String = {
    val hm = new HandshakeMessage()
    hm.setHandshake(h)
    mapper.writeValueAsString(hm)
  }
}