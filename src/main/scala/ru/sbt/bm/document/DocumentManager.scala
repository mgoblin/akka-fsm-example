package ru.sbt.bm.document

import akka.actor._
import akka.persistence.PersistentActor
import ru.sbt.bm.source.EndpointSupervisor

import scala.collection._

/**
 * Document manager.
 *
 * Persists document id's
 *
 * Forward state request to document actor;
 * Forward document event to document actor;
 * Return document list for request document list;
 *
 * Create and supervise document actors and endpoint manager.
 * Document actors name is equals to document number.
 */
class DocumentManager extends PersistentActor with DocumentManagerSupport {

  /** Unique persistence id*/
  def persistenceId = "documentsList"
  log.debug(s"Persistence id = $persistenceId")

  context.actorOf(Props[EndpointSupervisor], "endpointSupervisor")

  override def addToDocumentList(id: Int): Unit = {
    if (managedDocumentIds.add(id)) {
      // First event for document arrived
      persist(managedDocumentIds) {
        state => log.debug(s"New document with id $id arrived.")
      }
    }
  }

  /**
   * Recover managedDocumentIds from persistent storage
   */
  def receiveRecover: Receive = {
    case x: mutable.Set[_] =>
      log.debug(s"Recover state command arrived with $x")
      managedDocumentIds = x.asInstanceOf[mutable.Set[Int]]
  }

  /**
   * Actor receive
   *
   * Handle actor messages.
   *
   * @return receive
   */
  def receiveCommand: Receive = receiveEvent
}
