package ru.sbt.bm.document

/**
 * Document events declarations
 *
 * Change of document events:
 * Each document can be
 *  created by user
 *  verified by manager
 *  and than executed by accountant
 *
 * Document creation - CreateDocument
 * Document verification - VerifyDocument
 * Document execution - ExecuteDocument
 *
 * User can request document state with DocumentStateRequest
 * User can request existing documents list with DocumentsList
*/

/**
 * Base trait for document events.
 *
 * each document have id
**/
trait DocumentEvent {
  /** document id */
  val id: Int
}
case class CreateDocument(id: Int) extends DocumentEvent
case class VerifyDocument(id: Int) extends DocumentEvent
case class ExecuteDocument(id: Int) extends DocumentEvent

/**
 * Document event extractor
 */
object DocumentEvent {
  /** All events extractor */
  def unapply(event: DocumentEvent): Option[DocumentEvent] = Some(event)
}

/** Document state request event */
case class DocumentStateRequest(id: Int)

/** Documents list request event */
case object DocumentsListRequest