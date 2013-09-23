package controllers

import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.Props
import akka.pattern._
import backend._
import play.api._
import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.json.Json._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future

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
  def conf = Action {
    request =>
      val images = Await.result((directoryActor ? DirectoryContent).mapTo[List[String]], 5.seconds)
      imageDir match {
        case Some(dir) => Ok(toJson(Map("image.dir" -> images)))
        case None => ServiceUnavailable(toJson(Map("error" -> "image.dir not set in application.conf")))
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