package ru.sbt.bm.source

import akka.actor._

/**
 * Endpoints supervisor.
 *
 * Create MQ and REST endpoints.
 *
 * On child actor exception stops "bad" actor
 */
class EndpointSupervisor extends Actor with ActorLogging {

  val mqEndpoint = context.actorOf(Props(new MQEndpoint("wmq:AKKA.IN", "wmq:AKKA.OUT")), "mq")
  val restEndpoint = context.actorOf(Props(new RestEndpoint(self)), "rest")

  def receive = { case msg =>
      log.debug(s"Forward $msg to document manager")
      context.parent forward msg
  }
}
