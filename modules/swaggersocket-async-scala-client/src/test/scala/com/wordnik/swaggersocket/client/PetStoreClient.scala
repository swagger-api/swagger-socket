package com.wordnik.swaggersocket.client

import java.util.Date
import java.io.Closeable
import org.json4s.jackson.JsonMethods
import akka.dispatch.{Promise, Future}


case class Pet(id: Long, category: Category, name: String, photoUrls: List[String], tags: List[Tag], status: String)
case class Tag(id: Long, name: String)
case class Category(id: Long, name: String)

case class User(
  id: Long = 0,
  username: String,
  firstName: String,
  lastName: String,
  email: String,
  password: String,
  phone: String,
  userStatus: Int)

case class Order(
  id: Long,
  petId: Long,
  quantity: Int,
  shipDate: Date,
  status: String)



class SwaggerApiClient(config: SwaggerConfig) extends Closeable {

  val baseUrl = config.baseUrl
  val dataFormat = config.dataFormat

  private[this] val client = transportClient

  protected def transportClient = new RestClient(config)

  val pets = new PetsApiClient(client, config)

  val store = new StoreApiClient(client, config)

  val user = new UserApiClient(client, config)

  def close() {
    client.close()
  }
}

abstract class ApiClient(client: TransportClient, config: SwaggerConfig) extends JsonMethods {
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

class PetsApiClient(client: TransportClient, config: SwaggerConfig) extends ApiClient(client, config) {

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

class StoreApiClient(client: TransportClient, config: SwaggerConfig) extends ApiClient(client, config) {

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

class UserApiClient(client: TransportClient, config: SwaggerConfig) extends ApiClient(client, config) {

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