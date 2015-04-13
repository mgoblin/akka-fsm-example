package ru.sbt.bm.source

import akka.actor.{ActorSystem, Props}
import akka.camel.{CamelExtension, CamelMessage}
import akka.testkit._
import org.apache.camel.component.mock.MockEndpoint
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import ru.sbt.bm.document._

import scala.concurrent.duration._
import scala.language.postfixOps

class MQEndpointSpec extends TestKit(ActorSystem("TestKitUsageSpec"))
  with FlatSpecLike with Matchers with DefaultTimeout with ImplicitSender
  with BeforeAndAfterAll {

  behavior of "MQEndpoint"

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  it should "receive input message" in {
    val probe = TestProbe()

    val mockOutput = "mock:foo"
    val resultEndpoint = CamelExtension.get(system).context.getEndpoint(mockOutput, classOf[MockEndpoint])
    val actorRef = TestActorRef(Props(new MQEndpoint("direct:in", mockOutput)), probe.ref, "mq")
    resultEndpoint.expectedMessageCount(0)

    val message = new CamelMessage(
      MQEndpoint.DocumentsListFormat,
      Map[String, Any]()
    )

    within(500 millis) {
      actorRef ! message
      probe.expectMsg(DocumentsListRequest)
    }

    resultEndpoint.assertIsSatisfied()
  }

  it should "forward replies to output queue" in {
    val mockOutput = "mock:foo"
    val resultEndpoint = CamelExtension.get(system).context.getEndpoint(mockOutput, classOf[MockEndpoint])
    val actorRef = TestActorRef(new MQEndpoint("direct:in", mockOutput))
    resultEndpoint.expectedMessageCount(1)

    within(500 millis) {
      actorRef ! "Reply"
      expectNoMsg()
    }

    resultEndpoint.assertIsSatisfied()
  }

  it should "Parse CreateDocument event and send to parent" in {
    val probe = TestProbe()
    val actorRef = TestActorRef(Props(new MQEndpoint("direct:in", "direct:out")), probe.ref, "mq")

    val newDocumentEvent = "New 10"
    val message = new CamelMessage(
      newDocumentEvent,
      Map[String, Any]()
    )

    within(500 millis) {
      actorRef ! message
      probe.expectMsg(CreateDocument(10))
    }
  }

  it should "Parse VerifyDocument event and send to parent" in {
    val probe = TestProbe()
    val actorRef = TestActorRef(Props(new MQEndpoint("direct:in", "direct:out")), probe.ref, "mq")

    val verifyDocumentEvent = "Verify 100"
    val message = new CamelMessage(
      verifyDocumentEvent,
      Map[String, Any]()
    )

    within(500 millis) {
      actorRef ! message
      probe.expectMsg(VerifyDocument(100))
    }
  }

  it should "Parse ExecuteDocument and send to parent" in {
    val probe = TestProbe()
    val actorRef = TestActorRef(Props(new MQEndpoint("direct:in", "direct:out")), probe.ref, "mq")

    val executeDocumentEvent = "Execute 2"
    val message = new CamelMessage(
      executeDocumentEvent,
      Map[String, Any]()
    )

    within(500 millis) {
      actorRef ! message
      probe.expectMsg(ExecuteDocument(2))
    }
  }

  it should "Parse StateDocument event and send to parent" in {
    val probe = TestProbe()
    val actorRef = TestActorRef(Props(new MQEndpoint("direct:in", "direct:out")), probe.ref, "mq")

    val executeDocumentEvent = "State 21"
    val message = new CamelMessage(
      executeDocumentEvent,
      Map[String, Any]()
    )

    within(500 millis) {
      actorRef ! message
      probe.expectMsg(DocumentStateRequest(21))
    }
  }

}
