package ru.sbt.bm.source

import akka.actor.ActorLogging
import akka.camel.{CamelExtension, CamelMessage, Consumer}
import ru.sbt.bm.document._

object MQEndpoint {
  /** New document command regex format **/
  val NewDocumentFormat = """New\s([0-9]+)""".r
  /** Verify document command regex format **/
  val VerifyDocumentFormat = """Verify\s([0-9]+)""".r
  /** Execute document command regex format **/
  val ExecuteDocumentFormat = """Execute\s([0-9]+)""".r
  /** Get document state command regex format**/
  val StateDocumentFormat = """State\s([0-9]+)""".r
  /** Get documents list command regex formatКоманда получения списка документов*/
  val DocumentsListFormat: String = "Documents"
}
/**
 * MQ endpoint actor.
 * With Apache Camel read input queue, parse messages to document events and send events to endpoint supervisor.
 *
 * By default read messages from queue configured at endpointUri
 * MQ manager params configured at spring.xml
 *
 * @param endpointUri MQ request queue Camel URI
 * @param responseUri MQ response queue Camel URI
 */
class MQEndpoint(
  val endpointUri: String,
  val responseUri: String
)
extends Consumer
with ActorLogging {

  import ru.sbt.bm.source.MQEndpoint._

  def receive = {
    // Receive messages from input queue
    case camelMessage: CamelMessage =>
      val mqMessage = camelMessage.getBodyAs(classOf[String], camelContext)
      log.debug(s"Message $mqMessage received from $endpointUri")
      val template = CamelExtension(context.system).template
      mqMessage match {
        case NewDocumentFormat(id) => context.parent ! CreateDocument(id.toInt)
        case VerifyDocumentFormat(id) => context.parent ! VerifyDocument(id.toInt)
        case ExecuteDocumentFormat(id) => context.parent ! ExecuteDocument(id.toInt)
        case StateDocumentFormat(id) => context.parent ! DocumentStateRequest(id.toInt)
        case DocumentsListFormat => context.parent ! DocumentsListRequest
        case _ => log.debug(s"Unknown command $mqMessage")
      }
    // Write responses to output queue
    case response => CamelExtension(context.system).template.sendBody(responseUri, response.toString)
  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.debug(s"MQEndpoint with uri $endpointUri stopped ")
    super.postStop()
  }
}