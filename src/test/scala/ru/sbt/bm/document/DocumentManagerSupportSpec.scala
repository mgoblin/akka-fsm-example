package ru.sbt.bm.document

import akka.testkit._
import akka.actor.{ActorRef, Props, Actor, ActorSystem}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.language.postfixOps
import akka.pattern.ask
import scala.util.Success

class DocumentManagerSupportSpec extends TestKit(ActorSystem("TestSystem"))
  with ImplicitSender
  with DefaultTimeout
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  val probe = TestProbe()
  val manager: TestActorRef[DocumentManagerSupport] = TestActorRef(Props(new DocumentManagerSupport {
    def receive: Actor.Receive = receiveEvent
    override def getOrCreateActor(id: Int): ActorRef = probe.ref
  }))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "Document manager actor" should {
    "On call DocumentsListRequest return document id's list" in {
      val result = (manager ? DocumentsListRequest).mapTo[List[Int]].value
      val Success(emptyDocs: List[Int]) = result.get
      emptyDocs shouldBe empty

      val documentID = 10
      manager.underlyingActor.addToDocumentList(documentID)
      val Success(docs: List[Int]) = (manager ? DocumentsListRequest).mapTo[List[Int]].value.get
      docs shouldEqual List(documentID)
    }


    "forward document create event to it's document actors" in {
      val event = CreateDocument(10)
      manager ! event
      probe.expectMsg(event)
    }

    "forward document verify event to it's document actors" in {
      val event = VerifyDocument(10)
      manager ! event
      probe.expectMsg(event)
    }

    "forward document execute event to it's document actors" in {
      val event = ExecuteDocument(10)
      manager ! event
      probe.expectMsg(event)
    }

    "forward document state request to it's document actors" in {
      val event = DocumentStateRequest(10)
      manager ! event
      probe.expectMsg(event)
    }

    "Does not forward unknown requests document actors" in {
      val event = "Hello"
      manager ! event
      probe.expectNoMsg()
    }
  }
}
