package ru.sbt.bm.source

import ru.sbt.bm.document._
import spray.json._

/**
 * Protocol for converting Scala classes to JSON
 */
object DocumentJsonProtocol extends DefaultJsonProtocol {
  implicit object DocumentStateJsonFormat extends JsonFormat[DocumentState] {
    def write(state: DocumentState) = JsString(state.toString)
    def read(value: JsValue) = value match {
      case JsString(s) => s match {
        case "Uninitialized" => Uninitialized
        case "Created" => Created
        case "Verified" => Verified
        case "Executed" => Executed
      }
      case _ => throw new DeserializationException("Cannot parse Document State")
    }
  }
  implicit val impDocument = jsonFormat2(DocumentDTO)
}