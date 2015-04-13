package ru.sbt.bm.source

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.IO
import akka.pattern.ask
import ru.sbt.bm.document._
import spray.can.Http
import spray.http.MediaTypes._
import spray.http.{StatusCode, StatusCodes}
import spray.routing.HttpService

import scala.concurrent.Future

/**
 * REST endpoint actor.
 *
 * Using spray receive REST/JSON requests and forward it to endpoint supervisor for handling
 *
 * This is only actors integration part and business logic. All REST routing rules implements with [[RestCommandService]]
 */
class RestEndpoint(val parent: ActorRef) extends Actor with  RestCommandService with ActorLogging {
  def actorRefFactory = context
  def receive = runRoute(mainRoute)

  import ru.sbt.bm.Akka._

  /**
   * Get documents list
   *
   * Ask parent actor for documents list
   *
   * @return document id's list future
   */
  def getDocumentList: Future[List[Int]] = {
    log.debug("Get document list request API")
    (parent ? DocumentsListRequest).mapTo[List[Int]]
  }

  /**
   * Change document state
   *
   * Send change request for parent actor
   * @param event one of [[ru.sbt.bm.document.DocumentEvent]]
   * @return OK on success
   */
  def withAction(event: DocumentEvent): StatusCode = {
    log.debug(s"Change document event $event API")
    parent ! event
    StatusCodes.OK
  }

  /**
   * Get document state by id
   * @param id document id
   * @return DocumentDTO future with state field
   */
  def getDocumentState(id: Int): Future[DocumentDTO] = {
    log.debug(s"Get document state $id API")
    (parent ? DocumentStateRequest(id)).mapTo[DocumentDTO]
  }

  // bind REST to http
  IO(Http) ? Http.Bind(self, interface = "localhost", port = 9999)
  log.debug("REST/JSON API bind to http://localhost:9999/document")
}

/**
 * Request routing trait
 *
 * Implements REST/JSON API
 *
 * GET /document/all retrieve List[DocumentDTO]
 * GET /document/{id} retrieve DocumentDTO
 *
 * PUT /document/{id}/{create | verify | execute }  generate and send to handling DocumentEvent.
 *    Return "OK" as request receive confirmation to client
 *    Get "OK" result does not guaranteed that event will be correctly handled.
 */
trait RestCommandService extends HttpService {

  // Don't remove imports. This imports have implicit conversions
  import scala.concurrent.ExecutionContext.Implicits.global
  import ru.sbt.bm.source.DocumentJsonProtocol._
  import spray.httpx.SprayJsonSupport._

  def getDocumentList: Future[List[Int]]
  def withAction(event: DocumentEvent): StatusCode
  def getDocumentState(id: Int): Future[DocumentDTO]

  val mainRoute =
    pathPrefix("document") {
      path("all") {
        get {
          respondWithMediaType(`application/json`) { complete(getDocumentList) }
        }
      } ~
      pathPrefix(IntNumber) { documentId =>
        get {
          respondWithMediaType(`application/json`) { complete(getDocumentState(documentId)) }
        } ~
        put {
          path("create") { complete(withAction(CreateDocument(documentId))) } ~
          path("verify") { complete(withAction(VerifyDocument(documentId))) } ~
          path("execute") { complete(withAction(ExecuteDocument(documentId))) }
        }
      }
    }
}