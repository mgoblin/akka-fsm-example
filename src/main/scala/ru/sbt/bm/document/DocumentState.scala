package ru.sbt.bm.document

/**
* Document states.
*/
sealed trait DocumentState
/** Initial state. Document actor created, but state not initialized. **/
case object Uninitialized extends DocumentState {
  override def toString: String = "Uninitialized"
}
/** Payment document created **/
case object Created extends DocumentState {
  override def toString: String = "Created"
}
/** Payment document verified **/
case object Verified extends DocumentState {
  override def toString: String = "Verified"
}
/** Payment document  executed **/
case object Executed extends DocumentState {
  override def toString: String = "Executed"
}


/** Payment document pattern matching extractor */
object DocumentState {
  def unapply(state: DocumentState): Option[DocumentState] = Some(state)
}