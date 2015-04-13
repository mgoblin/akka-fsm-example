package ru.sbt.bm.document

import akka.persistence.PersistentActor

/**
 * Payment document.
 *
 * Each payment document has unique id and state.
 *
 * Document class is responsible for persistence and handling current state requests.
 * State transition delegated to [[DocumentFSM]] trait
 *
 * Payment document is a finite state machine with fast forwarding states and persistence support
 * State transitions
 * State(Created), Event(CreateDocument) -> State(Verified), timeout | Event(VerifyDocument) -> Executed
 *
 * @param id document unique id
*/
class Document(val id: Int) extends PersistentActor with DocumentFSM {

  /** Unique id for persistent storage */
  def persistenceId = id.toString

  log.debug(s"Persistence id = $id")

  // On document state transition change async persist document state.
  onTransition {
    case x =>
      log.debug(s"$x")
      persistAsync(Event(x._2, ())) {
        state => log.debug(s"State ${x._2} persisted for document $id")
      }
  }

  /**
   * Required by PersistentActor interface
   * @return  Unit
   */
  def receiveCommand = {
    case x => log.debug(s"Receive command $x")
  }

  /**
   * Recover document state
   * @return recovered state
   */
  def receiveRecover = {
    case Event(x@DocumentState(_), _) =>
      log.debug(s"Recover state command arrived $x")
      log.debug(s"Current state is $stateName and recovery running is $recoveryRunning")
      startWith(x, ())
  }
}