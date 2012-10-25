package com.wordnik.swaggersocket.client

import akka.util.Duration
import akka.util.duration._
import java.io.{StringWriter, Closeable}
import org.json4s.jackson.JsonMethods
import akka.dispatch.{Promise, Future}
import com.wordnik.model.{User, Order, Tag, Pet}
import com.wordnik.client.SwaggerConfig.DataFormat
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

class SwaggerApiClient(config: SwaggerConfig) extends Closeable {

  val baseUrl = config.baseUrl
  val dataFormat = config.dataFormat

  private[this] val client = new RestClient(config)

  val pets = new PetsApiClient(client, config)

  val store = new StoreApiClient(client, config)

  val user = new UserApiClient(client, config)

  def close() {
    client.close()
  }
}

abstract class ApiClient(client: RestClient, config: SwaggerConfig) extends JsonMethods {
  protected implicit val execContext = client.execContext
  protected val ser = config.dataFormat

  protected def addFmt(pth: String) = pth.replace("{format}", ser.name)

  protected def process[T](fn: => T): Future[T]  = {
    val fut = Promise[T]
    try {
      val r = fn
      r match {
        case t: Throwable => fut.complete(Left(t))
        case s => fut.complete(Right(r))
      }
    } catch {
      case t: Throwable => fut.complete(Left(t))
    }
    fut
  }
}

class PetsApiClient(client: RestClient, config: SwaggerConfig) extends ApiClient(client, config) {

  def getPetById(id: Long): Future[Pet] = {
    client.submit("GET", addFmt("/pet.{format}/") +id.toString, Map.empty, Map.empty, "") flatMap  { res =>
      process(ser.deserialize[Pet](res.body))
    }
  }

  def addPet(pet: Pet): Future[Pet] = {
    client.submit("POST", addFmt("/pet.{format}"), Map.empty, Map.empty, ser.serialize(pet)) flatMap { res =>
      process(ser.deserialize[Pet](res.body))
    }
  }

  def updatePet(pet: Pet): Future[Pet] = {
    client.submit("PUT", addFmt("/pet.{format}"), Map.empty, Map.empty, ser.serialize(pet)) flatMap { res =>
      process(ser.deserialize[Pet](res.body))
    }
  }

  def findPetsByStatus(status: List[String]): Future[List[Pet]] = {
    client.submit("GET", addFmt("/pet.{format}/findPetsByStatus"), Map("status" -> status.mkString(",")), Nil, "") flatMap { res =>
      process {
        ser.deserialize[List[Pet]](res.body)
      }
    }
  }

  def findPetsByTags(tags: Iterable[Tag]): Future[List[Pet]] = {
    client.submit("GET", addFmt("/pet.{format}/findByTags"), Map("tags" -> tags.map(_.name).mkString(",")), Nil, "") flatMap { res =>
      process {
        ser.deserialize[List[Pet]](res.body)
      }
    }
  }
}

class StoreApiClient(client: RestClient, config: SwaggerConfig) extends ApiClient(client, config) {

  def getOrderById(id: Long): Future[Order] = {
    client.submit("GET", addFmt("/store.{format}/order/") + id.toString, Map.empty, Map.empty, null) flatMap { res =>
      process(ser.deserialize[Order](res.body))
    }
  }

  def deleteOrder(id: Long): Future[Unit] = {
    client.submit("DELETE", addFmt("/store.{format}/order/" + id.toString), Map.empty, Map.empty, null) flatMap { _ => process(())}
  }

  def placeOrder(order: Order): Future[Unit] = {
    client.submit("POST", addFmt("/store.{format}/order"), Map.empty, Map.empty, null) flatMap { _ => process(()) }
  }
}

class UserApiClient(client: RestClient, config: SwaggerConfig) extends ApiClient(client, config) {

  def createUsersWithArrayInput(users: Array[User]): Future[Array[User]] = {
    client.submit("POST", addFmt("/user.{format}/createWithList"), Map.empty, Map.empty, ser.serialize(users.toList)) flatMap { res =>
      process(ser.deserialize[List[User]](res.body).toArray)
    }
  }

  def createUser(user: User): Future[User] = {
    client.submit("POST", addFmt("/user.{format}"), Map.empty, Map.empty, ser.serialize(user)) flatMap { res =>
      process(ser.deserialize[User](res.body))
    }
  }

  def createUsersWithListInput(users: List[User]): Future[List[User]] = {
    client.submit("POST", addFmt("/user.{format}/createWithList"), Map.empty, Map.empty, ser.serialize(users)) flatMap { res =>
      process(ser.deserialize[List[User]](res.body))
    }
  }

  def updateUser(username: String, user: User): Future[User] = {
    client.submit("PUT", addFmt("/user.{format}/") + username, Map.empty, Map.empty, ser.serialize(user)) flatMap { res =>
      process(ser.deserialize[User](res.body))
    }
  }

  def getUserByName(username: String): Future[User] = {
    client.submit("GET", addFmt("/user.{format}/" + username), Map.empty, Map.empty, "") flatMap { res =>
      process(ser.deserialize[User](res.body))
    }
  }


  def deleteUser(username: String): Future[Unit] = {
    client.submit("DELETE", addFmt("/user.{format}/" + username), Map.empty, Map.empty, "") flatMap { _ => process(()) }
  }

  def loginUser(username: String, password: String): Future[User] = {
    client.submit("GET", addFmt("/user.{format}/login"), Map("username" -> username, "password" -> password), Map.empty, "") flatMap { res =>
      process(ser.deserialize[User](res.body))
    }
  }

  def logoutUser(): Future[Unit] = {
    client.submit("GET", addFmt("/user.{format}/logout"), Map.empty, Map.empty, "") flatMap { _ => process(()) }
  }
}