package com.wordnik.swaggersocket.client

import akka.util.Duration
import akka.util.duration._
import java.io.StringWriter
import org.json4s.jackson.JsonMethods
import SwaggerConfig.DataFormat
import org.json4s._
import org.json4s.Xml._
import io.Codec

object SwaggerConfig {
  sealed trait DataFormat {

    def name: String
    def contentType: String

    def serialize[T](obj: T): String
    def deserialize[T:Manifest](json: String): T
    def deserialize[T:Manifest](json: JValue): T
  }
  object DataFormat {
    object Json {
      def apply(fmts: Formats) = new Json(fmts)
    }
    class Json(fmts: Formats) extends DataFormat {
      implicit protected val jsonFormats: Formats = fmts
      val contentType: String = "application/json;charset=utf-8"

      val name: String = "json"

      def deserialize[T: Manifest](json: String): T = JsonMethods.parse(json, useBigDecimalForDouble = true).extract[T]
      def deserialize[T: Manifest](json: JValue): T = json.extract[T]

      def serialize[T](obj: T): String = JsonMethods.compact(JsonMethods.render(Extraction.decompose(obj)))
    }

    object XML {
      def apply(fmts: Formats) = new XML(fmts)
    }
    class XML(fmts: Formats) extends DataFormat {
      implicit protected val jsonFormats: Formats = fmts
      val contentType: String = "application/xml"

      val name: String = "xml"

      def deserialize[T: Manifest](json: String): T = {
        val JObject(JField(_, jv) :: Nil) = toJson(scala.xml.XML.loadString(json))
        jv.extract[T]
      }
      def deserialize[T: Manifest](json: JValue): T = json.extract[T]

      protected lazy val xmlRootNode = <resp></resp>

      def serialize[T](obj: T): String = {
        val json = Extraction.decompose(obj)
        val sw = new StringWriter()
        scala.xml.XML.write(sw, xmlRootNode.copy(child = toXml(json)), Codec.UTF8.name, true, null)
        sw.toString
      }
    }
  }
}
case class SwaggerConfig(
  baseUrl: String,
  userAgent: String = RestClient.DefaultUserAgent,
  dataFormat: SwaggerConfig.DataFormat = DataFormat.Json(DefaultFormats),
  idleTimeout: Duration = 5 minutes,
  maxMessageSize: Int = 8912,
  enableCompression: Boolean = true,
  followRedirects: Boolean = true,
  identity: String = "0")
