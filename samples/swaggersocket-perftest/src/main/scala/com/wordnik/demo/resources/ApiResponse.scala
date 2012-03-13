package com.wordnik.demo.resources

import javax.xml.bind.annotation._

import scala.reflect.BeanProperty

@XmlRootElement
class ApiResponse {
  def this(c:Int, m:String) = {
    this()
    code = c
    message = m
  }
  @BeanProperty var message:String = _
  @BeanProperty var code:Int = 0
}