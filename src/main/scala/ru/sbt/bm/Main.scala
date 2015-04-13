package ru.sbt.bm

import akka.actor.{ActorSystem, Props}
import akka.camel.CamelExtension
import akka.util.Timeout
import org.apache.camel.spring.spi.ApplicationContextRegistry
import org.springframework.context.support.ClassPathXmlApplicationContext
import ru.sbt.bm.Akka._
import ru.sbt.bm.document.DocumentManager

import scala.concurrent.duration._

/**
 * Standalone application entry point
 *
 * It have main method running as standalone java se application.
 */
object Main {

  /**
   * Java SE application entry point
   *
   * Initialize actor system and Akka Camel integration.
   *
   * @param args  command line arguments. Not used.
   *              Actors configured with application.conf
   *              Apache Camel configured with spring.xml
   *              Logging configured with log4j.xml
   */
  def main(args: Array[String]) {
    // Init Camel with Spring support
    val springCtx = new ClassPathXmlApplicationContext("spring.xml")
    val camel = CamelExtension.get(system)
    camel.context.setRegistry(new ApplicationContextRegistry(springCtx))

    // Init root actor
    system.actorOf(Props[DocumentManager], "documentManager")
  }
}

/**
 * Global Akka parameters container
 */
object Akka {

  // Akka actors system
  implicit val system = ActorSystem("ActorSystem")

  // Default timeout
  implicit val timeout = Timeout(25.seconds)
}
