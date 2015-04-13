package ru.sbt.bm.document

import akka.actor.ActorSystem
import akka.actor.FSM.StateTimeout
import akka.testkit._
import org.scalatest.{Matchers, WordSpecLike}
import scala.concurrent.Await
import akka.pattern.ask
import scala.concurrent.duration._
import scala.language.postfixOps
import java.util.concurrent.TimeoutException

class DocumentFSMSpec extends TestKit(ActorSystem("TestSystem"))
  with ImplicitSender
  with DefaultTimeout
  with WordSpecLike with Matchers {

  private val documentId = 1

  """Document actor have three valid states
    | Created
    | Verified
    | Executed
    |
    | On DocumentEvent's actor move to states Created -> Verified -> Executed.
    | Actor state can move "fast forward", but can't move to previous state
  """.stripMargin should {
    "After creation actor have Uninitialized state" in {
      val fsm = TestFSMRef(new DocumentFSM {val id = documentId})
      fsm shouldNot be(null)
      fsm.stateName shouldEqual Uninitialized
    }

    "On Uninitialized state" must {
      "When CreateDocument event arrived document change state to Created" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm ! CreateDocument(documentId)
        fsm shouldNot be(null)
        fsm.stateName shouldEqual Created
      }

      "When VerifyDocument event arrived document change state to Verified" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm ! VerifyDocument(documentId)
        fsm shouldNot be(null)
        fsm.stateName shouldEqual Verified
      }

      "When ExecuteDocument change state to Executed" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm ! ExecuteDocument(documentId)
        fsm shouldNot be(null)
        fsm.stateName shouldEqual Executed
      }
    }

    "On Created state" must {
      "When CreateDocument event arrived dont change state" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm.setState(Created, ())

        fsm ! CreateDocument(documentId)
        fsm shouldNot be(null)
        fsm.stateName shouldEqual Created
      }

      "When VerifyDocument event arrived change state to Verified" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm.setState(Created, ())

        fsm ! VerifyDocument(documentId)
        fsm shouldNot be(null)
        fsm.stateName shouldEqual Verified
      }

      "When ExecuteDocument event arrived change state to Executed" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm.setState(Created, ())

        fsm ! ExecuteDocument(documentId)
        fsm shouldNot be(null)
        fsm.stateName shouldEqual Executed
      }

    }

    "On Verified state" must {
      "On CreateDocument event arrived stay in Verified state" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm.setState(Verified, ())

        fsm ! CreateDocument(documentId)
        fsm shouldNot be(null)
        fsm.stateName shouldEqual Verified
      }

      "On VerifyDocument event arrived stay in Verified state" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm.setState(Verified, ())

        fsm ! VerifyDocument(documentId)
        fsm shouldNot be(null)
        fsm.stateName shouldEqual Verified
      }

      "On ExecuteDocument event arrived stay in Executed state" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm.setState(Verified, ())

        fsm ! ExecuteDocument(documentId)
        fsm shouldNot be(null)
        fsm.stateName shouldEqual Executed
      }

      "On timeout change state to Executed" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm.setState(Verified, ())
        fsm ! StateTimeout
        fsm shouldNot be(null)
        fsm.stateName shouldEqual Executed
      }
    }

    "On Executed state" must {
      "On any event arrived stay in Executed state" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm.setState(Executed, ())

        fsm ! CreateDocument(documentId)
        fsm shouldNot be(null)
        fsm.stateName shouldEqual Executed

        fsm ! VerifyDocument(documentId)
        fsm shouldNot be(null)
        fsm.stateName shouldEqual Executed

        fsm ! ExecuteDocument(documentId)
        fsm shouldNot be(null)
        fsm.stateName shouldEqual Executed
      }
    }

    "At any state on any event check event id == this document id" must {

      "On Uninitialized state check id of CreateDocument event. Don't change state if id != document id" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = 5})
        fsm ! CreateDocument(3)
        fsm.stateName shouldEqual Uninitialized

        fsm ! CreateDocument(5)
        fsm.stateName shouldEqual Created
      }

      "On Created state check id of VerifyDocument event. Don't change state if id != document id" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = 5})
        fsm.setState(Created, ())
        fsm ! VerifyDocument(3)
        fsm.stateName shouldEqual Created

        fsm ! VerifyDocument(5)
        fsm.stateName shouldEqual Verified
      }

      "On Created state check id of ExecuteDocument event. Don't change state if id != document id" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = 5})
        fsm.setState(Created, ())
        fsm ! ExecuteDocument(3)
        fsm.stateName shouldEqual Created

        fsm ! ExecuteDocument(5)
        fsm.stateName shouldEqual Executed
      }

      "On Verified state check id of ExecuteDocument. Don't change state if id != document id" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = 5})
        fsm.setState(Verified, ())
        fsm ! ExecuteDocument(3)
        fsm.stateName shouldEqual Verified

        fsm ! ExecuteDocument(5)
        fsm.stateName shouldEqual Executed
      }
    }

    "At any state return on c state of document" must {
      "Uninitialized state return Uninitialized" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        val stateFuture = fsm ? DocumentStateRequest(documentId)
        val state = Await.result(stateFuture, 10 second).asInstanceOf[DocumentDTO].state
        state shouldEqual Uninitialized
      }

      "Created state return Created" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm.setState(Created)
        val stateFuture = fsm ? DocumentStateRequest(documentId)
        val state = Await.result(stateFuture, 10 second).asInstanceOf[DocumentDTO].state
        state shouldEqual Created
      }

      "Verified state return Verified" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm.setState(Verified)
        val stateFuture = fsm ? DocumentStateRequest(documentId)
        val state = Await.result(stateFuture, 10 second).asInstanceOf[DocumentDTO].state
        state shouldEqual Verified
      }

      "Executed state return Executed" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = documentId})
        fsm.setState(Executed)
        val stateFuture = fsm ? DocumentStateRequest(documentId)
        val state = Await.result(stateFuture, 10 second).asInstanceOf[DocumentDTO].state
        state shouldEqual Executed
      }

      "On Any state check id of DocumentStateRequest. Don't answer if id != document id" in {
        val fsm = TestFSMRef(new DocumentFSM{val id = 5})
        an[TimeoutException] shouldBe thrownBy(Await.result(fsm ? DocumentStateRequest(3), 1 second).asInstanceOf[DocumentDTO].state)

        fsm.setState(Created)
        an[TimeoutException] shouldBe thrownBy(Await.result(fsm ? DocumentStateRequest(3), 1 second).asInstanceOf[DocumentDTO].state)

        fsm.setState(Verified)
        an[TimeoutException] shouldBe thrownBy(Await.result(fsm ? DocumentStateRequest(3), 1 second).asInstanceOf[DocumentDTO].state)

        fsm.setState(Executed)
        an[TimeoutException] shouldBe thrownBy(Await.result(fsm ? DocumentStateRequest(3), 1 second).asInstanceOf[DocumentDTO].state)
      }
    }
  }
}