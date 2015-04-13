package ru.sbt.bm.document

/**
 * Document data transfer object.
 *
 * Using for non actor representation of document.
 * For example REST/JSON
 */
case class DocumentDTO(
  /** Document id */
  id: Int,
  /** Document state */
  state: DocumentState
) {
  require(state != null, "State must not be null")
}
