package com.wordnik.swagger.sample.model

import com.wordnik.swagger.annotations._

import java.util.List
import java.util.ArrayList

import javax.xml.bind.annotation._
import scala.reflect.BeanProperty

@XmlRootElement(name = "Pet")
@XmlAccessorType(XmlAccessType.NONE)
class Pet() {
  @XmlElement(name = "id") @BeanProperty var id: Long = 0
  @XmlElement(name = "category") @BeanProperty var category: Category = null
  @XmlElement(name = "name") @BeanProperty var name: String = null
  @XmlElement(name = "photoUrls") @BeanProperty var photoUrls: List[String] = new ArrayList[String]()
  @XmlElement(name = "tags") @BeanProperty var tags: List[Tag] = new ArrayList[Tag]()
  @XmlElement(name = "status")
  @ApiProperty(value = "pet status in the store", allowableValues = "available,pending,sold") @BeanProperty var status: String = null
}