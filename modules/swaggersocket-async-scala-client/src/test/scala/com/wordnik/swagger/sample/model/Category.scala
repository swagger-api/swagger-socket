package com.wordnik.swagger.sample.model

import javax.xml.bind.annotation.{XmlRootElement, XmlElement}

@XmlRootElement(name = "Category")
class Category() {

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