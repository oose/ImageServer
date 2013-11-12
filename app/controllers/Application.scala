package controllers

import java.io.File
import java.util.Calendar
import java.util.GregorianCalendar
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import org.apache.http.impl.cookie.DateUtils
import play.api._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc._
import akka.actor._
import akka.actor.actorRef2Scala
import akka.pattern.ask
import backend.DirectoryActor
import backend.DirectoryActor.Evaluation
import backend.DirectoryActor.EvaluationAccepted
import backend.DirectoryActor.EvaluationRejected
import backend.DirectoryActor.EvaluationStatus
import backend.DirectoryActor.RequestImage
import backend.DirectoryActor.StatusRequest
import backend.DirectoryActor.StatusResponse
import model.Image
import backend.StatusReportActor
import oose.play.config.Configured
import util.AppConfig
import util.Implicits.statusReponseJson
import play.api.cache.Cached

object Application extends Controller with Configured {

  import DirectoryActor._

  val appConfig = configured[AppConfig]

  implicit val actorSystem = Akka.system

  implicit val timeout = akka.util.Timeout(5.seconds)

  val directoryActor = actorSystem.actorOf(DirectoryActor.props, DirectoryActor.name)

  val statusReportActor = actorSystem.actorOf(StatusReportActor.props(directoryActor), "StatusReportActor")

  /**
   * compute the next year for use in EXPIRES cache settings
   */
  private def nextYear() = {
    val calendar = new GregorianCalendar();
    calendar.add(Calendar.YEAR, 1);
    DateUtils.formatDate(calendar.getTime());
  }

  /**
   * compute the imagepath on this host for a given request and image id.
   */
  private def imagePath[A](request: Request[A], id: String) =
    "http://" + request.host + request.path + "/" + id

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
      Ok(toJson(Map("success" -> s"server ${request.host} is alive")))
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
      val response = (directoryActor ? RequestImage).mapTo[Option[Image]]
      response.map(
        _ match {
          case Some(image) => Ok(toJson(Map("id" -> imagePath(request, image.id))))
          case None => BadRequest(toJson(Map("error" -> s"No more files available on ${request.host} ")))
        })
  }

  /**
   *  request an image with a particular Id.
   *  Send the specified file to the browser.
   *  @param id the file (without directory path)
   */
  def image(id: String) = Cached(id) {
    Action {
      request =>
        Logger.info(s"""
          requested image for ${id}
          """)
        appConfig.imageDir match {
          case Some(dir) =>
            val file = new File(dir + "/" + id)
            file.exists() match {
              case true => Ok.sendFile(content = file, inline = true)
              case false => BadRequest
            }
          case None => BadRequest
        }
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
          received evaluation data for ${body} in image server controller.
          json : ${Json.prettyPrint(request.body)}
          
      """)
      (body \ "tags").asOpt[List[String]] match {
        case Some(tags) =>
          val response = (directoryActor ? Evaluation(id, tags)).mapTo[EvaluationStatus]
          response.map(r =>
            r match {
              case EvaluationAccepted => Ok(s"Evaluation accepted")
              case EvaluationRejected(reason) => BadRequest(reason)
            })

        case None => Future { BadRequest("value for tags not specified in body") }
      }
  }

  def die(msg: String) = Action {
    directoryActor ! msg
    Ok
  }

  /**
   * request new websocket channels from the [[backend.StatusReportActor]]
   * The reply will have the following format:
   * {{{
   * {
   * total: 30,
   * images: [
   * {
   * id: "DSC05737.jpg",
   * state: "is evaluated",
   * tags: [
   * "boot",
   * "test",
   * "whatever"
   * ]
   * },
   * {
   * id: "DSC05730.jpg",
   * state: "is evaluated",
   * tags: [
   * "boot"
   * ]
   * },
   * {
   * id: "DSC05759.jpg",
   * state: "not evaluated"
   * },...
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