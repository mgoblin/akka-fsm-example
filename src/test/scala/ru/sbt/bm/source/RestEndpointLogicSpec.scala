package ru.sbt.bm.source

import akka.actor.{ActorSystem, Props}
import akka.testkit._
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import ru.sbt.bm.document._

class RestEndpointLogicSpec extends TestKit(ActorSystem("TestKitUsageSpec"))
  with FlatSpecLike with Matchers with DefaultTimeout with ImplicitSender
  with BeforeAndAfterAll  {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  behavior of "RestEndpoint"

  val probe = TestProbe()
  val actorRef = TestActorRef[RestEndpoint](Props(new RestEndpoint(probe.ref)), "restSupervisor")

  it should "on call getDocumentList ask documents list using DocumentsList message from parent actor (endpoint supervisor) " in {
    actorRef.underlyingActor.getDocumentList
    probe.expectMsg(DocumentsListRequest)
  }

  it should "on call getDocumentState ask document instance using DocumentStateRequest from parent actor (endpoint supervisor) " in {
    actorRef.underlyingActor.getDocumentState(20)
    probe.expectMsg(DocumentStateRequest(20))
  }

  it should "on call withAction send event to parent actor (endpoint supervisor)" in  {
    val events = Set(CreateDocument(1), VerifyDocument(3), ExecuteDocument(2))

    events.foreach { event =>
      actorRef.underlyingActor.withAction(event)
      probe.expectMsg(event)
    }
  }

}
