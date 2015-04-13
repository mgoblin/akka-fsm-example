package ru.sbt.bm.document

import org.scalatest.{FlatSpec, Matchers}

class DocumentDTOSpec extends FlatSpec with Matchers {
  behavior of "DocumentDTO"

  it should "store id and document state" in {
    val dto = DocumentDTO(1, Created)
    dto.id shouldEqual 1
    dto.state shouldEqual Created
  }

  it should "throw IllegalArgumentException on null document state" in {
    val thrown = intercept[IllegalArgumentException] {
      val dto = DocumentDTO(1, null)
    }
    assert(thrown.getMessage === "requirement failed: State must not be null")
  }
}
