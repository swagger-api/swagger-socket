package com.wordnik.swagger.sample.model

import javax.xml.bind.annotation._

@XmlRootElement(name = "Tag")
class Tag {
  private var id:Long = 0
  private var name:String = _

  @XmlElement(name="id")
  def getId():Long = {
    id
  }

  def setId(id:Long):Unit = {
    this.id = id
  }

  @XmlRootElement(name = "name")
  def getName():String = {
    name
  }

  def setName(name:String):Unit = {
    this.name = name
  }
}