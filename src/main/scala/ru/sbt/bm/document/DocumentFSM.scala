package ru.sbt.bm.document

import scala.language.postfixOps
import akka.actor.{FSM, ActorLogging}
import scala.concurrent.duration._
import DocumentFSM._

/**
 * Companion object for DocumentFSM trait
 *
 * Define finite state machine moves.
 * Document events moves document from current state to the next state.
 */
object DocumentFSM {
  /**
   * Ordered event to document state transitions
   */
  private[this] val statesTransitions: Seq[(Class[_ <: DocumentEvent], DocumentState)] = Seq(
    // CreateDocument move state to Created
    classOf[CreateDocument]  -> Created,
    // VerifyDocument move state to Created
    classOf[VerifyDocument] -> Verified,
    // ExecuteDocument move state to Created
    classOf[ExecuteDocument] -> Executed
  )

  /**
   * Get state for document event
   * @param event event
   * @return document state and index pair for event. If event not contains in statesTransitions return (-1, Uninitialized) state pair
   */
  def stateForEvent(event: DocumentEvent): (Int, DocumentState) = statesTransitions.find( _._1 == event.getClass ) match {
    case Some((clazz, s)) => (statesTransitions.indexOf((clazz, s)), s)
    case None => (-1, Uninitialized)
  }

  def stateIndex(state: DocumentState): Int = statesTransitions.find(_._2 == state) match {
    case Some((clazz, s)) => statesTransitions.indexOf((clazz, s))
    case None => -1
  }

}

/**
 * Payment document state finite state machine
 *
 * Document have three states
 * Created
 * Verified
 * Executed
 *
 * Implements FSM Created -> Verified -> Executed.
 * Document state can move only forward with fast moves.
 * For example from Created to Executed on ExecuteDocument event arrived.
 * FSM wait 10 seconds and than move state to Executed
 *
 */
trait DocumentFSM extends FSM[DocumentState, Unit] with ActorLogging {

  /** Payment document id. At this train should be abstract. */
  val id: Int

  /**
   * Change document state.
   *
   * Check event id equals document id.
   * If event id != document id that means event routing to document error,  log it.
   *
   * If
   *
   * @param event document event
   * @return state move or stay
   */
  def moveToNextState(event: DocumentEvent): State =
    if (event.id == id) {
      log.debug(s"From $stateName go to ${stateForEvent(event)}")
      val (nextStateIdx, nextState) = stateForEvent(event)
      if (nextStateIdx <= stateIndex(stateName)) stay() else goto(nextState)
    } else {
      log.error(s"Document with id = $id receive event for id = ${event.id}")
      stay()
    }

  def stayInCurrentState(event: DocumentEvent): State = {
    if (event.id == id)
      log.warning("Document already Created")
    else
      log.error("Document with id = $id receive event for id = ${documentEvent.id}")
    stay()
  }

  // Initial state
  startWith(Uninitialized, ())

  // From Uninitialized state document can move forward to Created, Verified, Executed on document event arrive.
  when(Uninitialized) {
    case Event(documentEvent @ DocumentEvent(_), _) => moveToNextState(documentEvent)
  }

  // From state Created move to Verified by event VerifyDocument
  // From state Created move to Executed by event ExecuteDocument
  when(Created) {
    case Event(documentEvent @ CreateDocument(_), _) => stayInCurrentState(documentEvent)
    case Event(documentEvent @ DocumentEvent(_), _) => moveToNextState(documentEvent)
  }

  // From state Verified move to Executed by event ExecuteDocument or by 10 seconds timeout
  when(Verified, stateTimeout = 10 seconds) {
    case Event(documentEvent @ ExecuteDocument(_), _) => moveToNextState(documentEvent)
    // Timer reset
    case Event(documentEvent @ DocumentEvent(_), _) => stayInCurrentState(documentEvent)
    case Event(StateTimeout, _) =>
      log.debug("Form Verified go to Executed with timeout")
      goto(Executed)
  }

  // Final document state Executed. No moves anymore.
  when(Executed) {
    case Event(documentEvent @ DocumentEvent(_), _) => stayInCurrentState(documentEvent)
  }

  whenUnhandled {
    // Get document state
    case Event(DocumentStateRequest(documentId), _) =>
      if (documentId == id) {
        log.debug(s"Document $id state is $stateName")
        sender ! new DocumentDTO(id, stateName)
      } else log.error(s"Document with id = $id receive event for id = $documentId")
      stay()

    // Unknown request
    case x =>
      log.debug(s"Document receive unknown message $x")
      stay()
  }

  initialize()
}
