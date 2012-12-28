package com.wordnik.swagger.sample.data

import collection.mutable.ListBuffer
import com.wordnik.swagger.sample.model.{ Order }
import java.util.Date

object StoreData {
  val orders: ListBuffer[Order] = new ListBuffer[Order]()
  orders += createOrder(1, 1, 2, new Date(), "placed")
  orders += createOrder(2, 1, 2, new Date(), "delivered")
  orders += createOrder(3, 2, 2, new Date(), "placed")
  orders += createOrder(4, 2, 2, new Date(), "delivered")
  orders += createOrder(5, 3, 2, new Date(), "placed")

  def findOrderById(orderId: Long): Order = {
    for (order <- orders) {
      if (order.getId() == orderId) {
        return order
      }
    }
    null
  }

  def placeOrder(order: Order): Unit = {
    // remove any pets with same id
    orders --= orders.filter(o => o.getId == order.getId)
    orders += order
  }

  def deleteOrder(orderId: Long): Unit = {
    orders --= orders.filter(o => o.getId == orderId)
  }

  private def createOrder(id: Long, petId: Long, quantity: Int, shipDate: Date, status: String): Order = {
    val order = new Order()
    order.setId(id)
    order.setPetId(petId)
    order.setQuantity(quantity)
    order.setShipDate(shipDate)
    order.setStatus(status)
    order
  }
}