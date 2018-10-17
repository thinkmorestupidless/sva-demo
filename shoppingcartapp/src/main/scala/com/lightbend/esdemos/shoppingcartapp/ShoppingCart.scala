package com.lightbend.esdemos.shoppingcartapp

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.lightbend.cinnamon.akka.CinnamonMetrics
import com.lightbend.cinnamon.metric.Counter
import spray.json._

object ShoppingCart {
  /** props class for shopping cart */
  def props(): Props = Props(new ShoppingCart())

  /** base type for shopping cart messages */
  sealed trait ShoppingCartMessage {
    val userId: String
  }

  /** base class for commit cart response */
  abstract class CommitCartResponse

  /**
    * contents of shipping cart
    *
    * @param userId    user id
    * @param status    status of shopping cart
    * @param timestamp timestamp of when this cart was last modified, also used for version checking
    * @param items     productId and quantities in cart
    */
  case class ShoppingCartContent(
    userId: String,
    status: ShoppingCart.CartStatus.Value,
    timestamp: Long,
    items: Map[String, Int]
  )

  /** simple record holder used for cart commits */
  case class CartId(userId: String, timestamp: Long)

  /**
    * clears shopping cart for user
    *
    * @return [CartCleared]
    */
  case class ClearCart(userId: String) extends ShoppingCartMessage

  /**
    * retrieve shopping cart contents
    *
    * @return message of type [ShoppingCartContent]
    */
  case class GetCartContent(userId: String) extends ShoppingCartMessage

  /** adds product ids to shopping cart */
  case class AddToCart(userId: String, products: List[String]) extends ShoppingCartMessage

  /**
    * commits shopping cart transaction
    *
    * @param userId    user id
    * @param timestamp timestamp of cart to commit
    * @return one of the following possible messages
    *         [CartCommitted] - if transaction successful
    *         [CartOutOfDate] - if shopping cart is stale (i.e. timestamp doesn't match)
    *         [CartEmpty]     - if cart is empty
    */
  case class CommitCart(override val userId: String, timestamp: Long) extends ShoppingCartMessage

  /** represents possible states of the shopping cart.  States other than Open are considered immutable */
  object CartStatus extends Enumeration {
    val Open, Committed = Value
  }

  /** json serialization support for shopping cart */
  object ShoppingCartJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {

    implicit object CartStatusFormats extends RootJsonFormat[CartStatus.Value] {
      def write(status: CartStatus.Value) = JsString(status.toString)

      def read(value: JsValue): CartStatus.Value = value match {
        case JsString(str) => CartStatus.withName(str)
        case _ => throw DeserializationException("CartStatus expected JsString")
      }
    }

    implicit val ShoppingCartFormats: RootJsonFormat[ShoppingCartContent] = jsonFormat4(ShoppingCartContent.apply)
    implicit val CardIdFormats: RootJsonFormat[CartId] = jsonFormat2(CartId.apply)
  }

  /** return message when cart cleared */
  case object CartCleared

  /** returned if cart has been committed */
  case object CartCommitted extends CommitCartResponse

  /** returned if cart is out of date */
  case object CartOutOfDate extends CommitCartResponse

  /** returned if cart is empty */
  case object CartEmpty extends CommitCartResponse

}

/**
  * contains the current state of the shopping cart for a given user as well as the history of shopping cart content
  */
class ShoppingCart extends Actor with ActorLogging {

  import ShoppingCart._

  val ProductsAddedMetric: Counter = CinnamonMetrics(context).createCounter("productsAdded")
  val CartsCommittedMetric: Counter = CinnamonMetrics(context).createCounter("cartsCommitted")
  val CartsOutOfDateMetric: Counter = CinnamonMetrics(context).createCounter("cartsOutOfDate")

  // current cart
  var currentCart: ShoppingCartContent = emptyCart()

  // list of committed cards, indexed by timestamp
  var committedCarts: Map[Long, ShoppingCartContent] = Map()

  def receive: Receive = {
    case ClearCart(_) =>
      currentCart = emptyCart()
      sender ! CartCleared
    case GetCartContent(_) =>
      sender ! currentCart
    case AddToCart(_, products) =>
      addProducts(products)
    case CommitCart(_, timestamp) =>
      commitCart(timestamp)
    case msg =>
      log.warning(s"unexpected message: {}", msg)
  }

  /** adds product list to cart */
  def addProducts(products: List[String]): Unit = {
    val updatedCart = products.foldLeft(currentCart.items) {
      case (accumCart, productId) =>
        accumCart.get(productId) match {
          case Some(count) => accumCart.updated(productId, count + 1)
          case None => accumCart.updated(productId, 1)
        }
    }
    currentCart = currentCart.copy(timestamp = System.currentTimeMillis(), items = updatedCart)
    ProductsAddedMetric.increment(products.size)
  }

  /** handles the commitment of a cart */
  def commitCart(timestamp: Long): Unit = {
    if (currentCart.items.isEmpty) {
      sender ! CartEmpty
    } else if (currentCart.timestamp != timestamp) {
      sender ! CartOutOfDate
      CartsOutOfDateMetric.increment()
    } else {
      val updatedCart = currentCart.copy(status = CartStatus.Committed, timestamp = timestamp)
      committedCarts = committedCarts.updated(timestamp, updatedCart)
      currentCart = emptyCart()
      sender ! CartCommitted
      CartsCommittedMetric.increment()
    }
  }

  /** creates empty cart */
  def emptyCart(): ShoppingCartContent = ShoppingCartContent(context.self.path.name, CartStatus.Open, System.currentTimeMillis(), Map())
}
