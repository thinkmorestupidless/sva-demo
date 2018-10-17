package com.lightbend.esdemos.shoppingcartapp

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

object TrafficGenerator {
  def props(clusterListener: ActorRef): Props = Props(new TrafficGenerator(clusterListener))
}

class TrafficGenerator(clusterListener: ActorRef) extends Actor with ActorLogging {

  val NumSessions: Int = context.system.settings.config.getInt("userSim.concurrent-users-per-node")
  var sessionCounter = 0

  override def preStart(): Unit = {
    (1 to NumSessions).foreach(_ => createShoppingSession())
  }

  def receive: Receive = {
    case Terminated(lastSession) =>
      createShoppingSession(Some(lastSession.path.name))
    case msg =>
      log.warning(s"unexpected message: {}", msg)
  }

  /** create a new shopping session */
  def createShoppingSession(sessionNameOpt: Option[String] = None): Unit = {
    val sessionName = sessionNameOpt match {
      case Some(name) =>
        name
      case None =>
        sessionCounter += 1
        s"shopping-session-actor-$sessionCounter"
    }

    val session = context.actorOf(ShoppingSession.props(sessionName, clusterListener), sessionName)
    context.watch(session)
  }
}
