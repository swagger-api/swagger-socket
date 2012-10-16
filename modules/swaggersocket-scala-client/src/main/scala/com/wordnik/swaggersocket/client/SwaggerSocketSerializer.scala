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

import org.codehaus.jackson.map.ObjectMapper
import com.wordnik.swaggersocket.protocol._

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

  def serializeClose(c : Close) : String = {
    val cm = new CloseMessage()
    cm.setClose(c)
    mapper.writeValueAsString(cm)
  }
}