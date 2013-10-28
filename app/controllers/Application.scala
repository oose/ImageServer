package controllers

import java.io.File

import scala.annotation.implicitNotFound
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import backend.DirectoryActor
import backend.Image
import backend.StatusReportActor
import util.Implicits._
import play.api._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc._

object Application extends Controller {

  import DirectoryActor._

  implicit val timeout = akka.util.Timeout(5.seconds)

  val directoryActor = Akka.system.actorOf(Props(new DirectoryActor(imageDir.get)), "DirectoryActor")

  val statusReportActor = Akka.system.actorOf(Props(new StatusReportActor(directoryActor)), "StatusReportActor")

  def imagePath[A](request: Request[A], id: String) = "http://" + request.host + request.path + "/" + id

  /**
   * get the configuration for the image directory.
   */
  private def imageDir: Option[String] = {
    Play.current.configuration.getString("image.dir")
  }

  /**
   * serve the main index page.
   */
  def index = Action {
    request => 
    Ok(views.html.index(request.host))
  }

  /**
   *  reply to a ping request.
   *  Example reply:
   *  {{{
   *  { "success" : "server http://localhost:9001 is alive" }
   *  }}}
   */
  def ping = Action {
    request =>
      val host = request.host
      Ok(toJson(Map("success" -> s"server $host is alive")))
  }

  /**
   *  get the configuration.
   *  Request the [[backend.DirectoryActor]] for
   *  the directory contents and a status request.
   *  Example reply:
   *  {{{
   *  {
   * image.dir: [
   * "DSC05637.jpg",
   * "DSC05642.jpg"
   * ],
   * totalImages: 2,
   * inEvaluation: 0
   * }
   * }}}
   *
   */
  def conf = Action.async {
    request =>
      val statusReponse = (directoryActor ? StatusRequest).mapTo[StatusResponse]

      statusReponse.map { sr =>
        Ok(toJson(sr))
      }
  }

  /**
   *  request the next ImageId.
   *  Ask the [[backend.DirectoryActor]] for a new image id to be processed.
   *  {{{
   * {
   * id: "http://localhost:9000/image/DSC05730.jpg"
   * }
   *  }}}
   */
  def imageId = Action.async {
    request =>
      val response = (directoryActor ? RequestImageId).mapTo[Option[Image]]
      response.map(
        _ match {
          case Some(image) => Ok(toJson(Map("id" -> imagePath(request, image.id))))
          case None => BadRequest(toJson(Map("error" -> "No more files available")))
        })
  }

  /**
   *  request an image with a particular Id.
   *  Send the specified file to the browser.
   *  @param id the file (without directory path)
   */
  def image(id: String) = Action {
    request =>
      Logger.info(s"""
          requested image for ${id}
          """)
      imageDir match {
        case Some(dir) =>
          val file = new File(dir + "/" + id)
          file.exists() match {
            case true => Ok.sendFile(content = file, inline = true)
            case false => BadRequest
          }
        case None => BadRequest
      }
  }

  /**
   *  provide the metadata for an image with a particular Id
   *  Post Json data with the metadata to the [[backend.DirectoryActor]]
   *  {{{
   *
   *  }}}
   *
   */
  def saveMetaData = Action.async(parse.json) {
    request =>
      val body = request.body
      val id = (body \ "id").as[String]
      Logger.info(s"""
          received evaluation data for ${body} in image server.
          json : ${Json.prettyPrint(request.body)}
          
      """)
      (body \ "tags").asOpt[List[String]] match {
        case Some(tags) =>
          val response = (directoryActor ? Evaluation(id, tags)).mapTo[EvaluationStatus]
          response.map(r =>
            r match {
              case EvaluationAccepted => Ok("Evaluation accepted")
              case EvaluationRejected(reason) => BadRequest(reason)
            })

        case None => Future { BadRequest("value for tags not specified in body") }
      }
  }

  /**
   * request new websocket channels from the [[backend.StatusReportActor]]
   * Reply will have the following format:
   * {{{
   * {
   * "total" : 30,
   * "unevaluated" : [ "DSC05737.jpg", "DSC05759.jpg", ... ],
   * "inEvaluation" : [ ],
   * "evaluated" : [ "DSC05730.jpg" ]
   * }
   * }}}
   */
  def ws = WebSocket.async[JsValue] {
    request =>
      // request new websocket channels from the statusReportActor
      val response = (statusReportActor ? StatusReportActor.RequestWebSocket).mapTo[StatusReportActor.WebSocketResponse]
      response.map {
        case StatusReportActor.WebSocketResponse(in, out) =>
          (in, out)
      }
  }
}