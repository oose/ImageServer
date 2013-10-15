package backend

import java.io.File

import scala.collection.JavaConversions.collectionAsScalaIterable

import org.apache.commons.io.FileUtils

import akka.actor._

class DirectoryActor(imageDir: String) extends Actor with ActorLogging {

  var images: List[String] = List.empty

  var imageActors: Map[String, ActorRef] = Map.empty

  /**
   * @return a list of graphic files in the directory.
   */
  def scanDirectory(dir: String): List[String] = {
    log.info("dir: " + dir)
    val files = FileUtils.listFiles(new File(dir), Array("jpg", "png", "gif"), false)
    files.map(_.getName()).toList
  }

  /**
   *  @return the first image which remains to be evaluated if any.
   */
  def imageId(): Option[String] = {
    images.diff(imageActors.keys.toList).headOption
  }

  override def preStart {
    images = scanDirectory(imageDir)
  }

  def receive = {
    case RequestId =>
      log.info("New Image Id requested")
      val id = imageId()
      id match {
        case Some(id) =>
          // create a new ImageActor - it will ping itself after a timeout,
          // e.g. when no evaluation happened
          val imageActor = context.actorOf(Props(new ImageActor(id)), id)
          imageActors = imageActors + (id -> imageActor)
        case None =>
      }
      // return the Option[String] to the sender
      sender ! id

    case DirectoryContent =>
      sender ! images

    case StatusRequest =>
      sender ! StatusResponse(images.size, imageActors.size)

    case Expired(id) =>
      log.info(s"An image expired, removing from queue : $id")
      context.stop(sender)
      imageActors = imageActors - id

    case Evaluation(id, tags) =>
      imageActors.get(id) match {
        case Some(image) =>
          image forward tags
        case None =>
          log.info(s"Evaluating image $id has expired")
          sender ! EvaluationRejected(s"Image $id has expired")
      }
  }

}
