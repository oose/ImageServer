package controllers

import java.io.File

import scala.annotation.implicitNotFound
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import akka.actor._
import akka.pattern.ask
import backend.DirectoryActor
import backend.DirectoryContent
import backend.Evaluation
import backend.EvaluationAccepted
import backend.EvaluationRejected
import backend.EvaluationStatus
import backend.RequestId
import backend.StatusRequest
import backend.StatusResponse
import play.api._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json.toJson
import play.api.mvc._

object Application extends Controller {

  implicit val timeout = akka.util.Timeout(5.seconds)

  val directoryActor = Akka.system.actorOf(Props(new DirectoryActor(imageDir.get)), "DirectoryActor")

  def imagePath[A](request: Request[A], id: String) = "http://" + request.host + request.path + "/" + id

  /**
   * get the configuration for the image directory.
   */
  def imageDir: Option[String] = {
    Play.current.configuration.getString("image.dir")
  }

  /**
   *  reply to a ping request.
   */
  def ping = Action {
    Ok
  }

  /**
   *  get the configuration
   */
  def conf = Action.async {
    request =>
      val directoryListing = (directoryActor ? DirectoryContent)
      val statusReponse = (directoryActor ? StatusRequest)

      val response = for (
        d <- directoryListing.mapTo[List[String]];
        s <- statusReponse.mapTo[StatusResponse]
      ) yield (d, s)

      response.map {
        case (directoryListing, StatusResponse(total, inEvaluation)) =>
          imageDir match {
            case Some(dir) =>
              Ok(toJson(Map("image.dir" -> toJson(directoryListing),
                "totalImages" -> toJson(total),
                "inEvaluation" -> toJson(inEvaluation))))
            case None =>
              ServiceUnavailable(toJson(Map("error" -> "image.dir not set in application.conf")))
          }
        case _ =>
          BadRequest
      }
  }

  /**
   *  request the next ImageId
   */
  def imageId = Action.async {
    request =>
      val response = (directoryActor ? RequestId).mapTo[Option[String]]
      response.map(
        _ match {
          case Some(id) => Ok(toJson(Map("id" -> imagePath(request, id))))
          case None => BadRequest(toJson(Map("error" -> "No more files available")))
        })
  }

  /**
   *  request an image with a particular Id
   */
  def image(id: String) = Action {
    request =>
      imageDir match {
        case Some(dir) =>
          val file = new File(dir + "/" + id)
          file.exists() match {
            case true => Ok.sendFile(file)
            case false => BadRequest
          }
        case None => BadRequest
      }
  }

  /**
   *  provide the metadata with for an image with a particular Id
   */
  def saveMetaData(id: String) = Action.async(parse.json) {
    request =>
      (request.body \ "tags").asOpt[List[String]] match {
        case Some(list) =>
          val response = (directoryActor ? Evaluation(id, list)).mapTo[EvaluationStatus]
          response.map(r =>
            r match {
              case EvaluationAccepted => Ok("Evaluation accepted")
              case EvaluationRejected(reason) => BadRequest(reason)
            })

        case None => Future { BadRequest("value for tags not specified in body") }
      }
  }
}