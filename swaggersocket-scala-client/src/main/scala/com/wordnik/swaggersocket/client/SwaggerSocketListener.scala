package com.wordnik.swaggersocket.client

import com.wordnik.swaggersocket.server.{Response, Request}

trait SwaggerSocketListener {

  def error(e : SwaggerSocketException) {}

  def message(req : Request, res: Response) {}

  def messages(res : java.util.List[Response]) {}

}