package ru.sbt.bm.document

import scala.collection.mutable
import akka.actor.{Actor, ActorLogging, Props, ActorRef}


/**
 * Document manager.
 *
 * Forward state request to document actor;
 * Forward document event to document actor;
 * Return document list for request document list;
 *
 * Document actors name is equals to document number.
 */
trait DocumentManagerSupport extends Actor with ActorLogging {
  /**
   * Managed document id's set.
   *
   * Store all document id's received by monitoring.
   * Document manager state is defined by document numbers
   *
   * Persisted to data storage
   */
  protected var managedDocumentIds: mutable.Set[Int] = mutable.Set()

  def addToDocumentList(id: Int): Unit = {
    managedDocumentIds.add(id)
  }

  /**
   * Get existing document actor ref or create new one on first call
   * @param id document id
   * @return ActorRef for document id
   */
  protected def getOrCreateActor(id: Int): ActorRef = context.child(id.toString) match {
    case Some(ref) =>
      log.debug(s"Find actor for Document with id = $id")
      ref
    case None =>
      log.debug(s"Instantiate Document actor id = $id")
      val documentActorRef = context.actorOf(Props(new Document(id)), id.toString)
      log.debug(s"Document actor name is ${documentActorRef.path.name}")
      documentActorRef
  }

  /**
   * Get or create actor ref for managed document actor by document id
   *
   * Get actor ref from document manager children actors.
   * Document actor create
   *   - on first document event arrive
   *   - after document manager restart
   *     -- on first state request
   *     -- document event arriving
   *
   * @param id  document id
   * @return document actor reference or None for non managed (not existed) documents.
   *         Document actors name is equals to document number.
   */
  protected def actorForDocument(id: Int): Option[ActorRef] = {
    // All document id's for managed documents contains in documentNumbers
    if (managedDocumentIds.contains(id)) {
      log.debug(s"Document id $id contains in managed document id set")
      Some(getOrCreateActor(id))
    } else {
      log.debug(s"Unknown document with id = $id")
      None
    }
  }

  def receiveEvent: Receive = {
    // Monitoring event arrived
    // Forward event to document actor
    case event: DocumentEvent =>
      log.debug(s"Document event arrived: $event")
      addToDocumentList(event.id)
      actorForDocument(event.id).get forward event

    // Document state request arrived
    // Forward request to monitored document actor or return Uninitialized state for not existed documents
    case DocumentStateRequest(id) => actorForDocument(id) match {
      case Some(document) =>
        log.debug(s"Forward DocumentStateRequest($id) to document actor")
        document forward DocumentStateRequest(id)
      case None => sender ! new DocumentDTO(id, Uninitialized)
    }

    // Monitored documents list request arrived
    // Response with managedDocumentIds
    case DocumentsListRequest =>
      log.debug("Document list request received")
      sender ! managedDocumentIds.toList

    // Unknown request
    case x => log.error(s"Unknown event $x")
  }
}
