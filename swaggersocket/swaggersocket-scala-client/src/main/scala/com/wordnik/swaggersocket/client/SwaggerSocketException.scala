package com.wordnik.swaggersocket.client

class SwaggerSocketException(status : Int, reasonPhrase : String) extends RuntimeException {

  def getStatusCode : Int = {
    status
  }

  def getReasonPhrase : String = {
    reasonPhrase
  }
}