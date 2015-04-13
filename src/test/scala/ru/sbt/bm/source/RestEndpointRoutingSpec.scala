package ru.sbt.bm.source

import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import ru.sbt.bm.document._
import spray.http.StatusCodes
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Future

class RestEndpointRoutingSpec
  extends FlatSpec with Matchers with BeforeAndAfterAll
  with ScalatestRouteTest with RestCommandService {

  // Stubs for calling outer actors
  val documentList = List(1)
  override def getDocumentList = Future { documentList }
  override def withAction(event: DocumentEvent) = StatusCodes.OK
  override def getDocumentState(id: Int) = Future { DocumentDTO(id, Created) }

  def actorRefFactory = system
  def context = null

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  behavior of "RestEndpoint"

  it should "route GET /document/all request to getDocumentList" in {
    Get("/document/all") ~> mainRoute ~> check {
      responseAs[String] shouldEqual "[1]"
    }
  }

  it should "route GET /document/{id} to getDocumentState" in {
    Get("/document/1") ~> mainRoute ~> check {
      responseAs[String] shouldEqual "{\n  \"id\": 1,\n  \"state\": \"Created\"\n}"
    }
  }

  it should "route PUT /document/{id}/create to withAction" in {
    Put("/document/1/create") ~> mainRoute ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }

  it should "route PUT /document/{id}/verify to withAction" in {
    Put("/document/1/verify") ~> mainRoute ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }

  it should "route PUT /document/{id}/execute to withAction" in {
    Put("/document/1/execute") ~> mainRoute ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }
}
