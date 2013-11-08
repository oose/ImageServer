package backend

import java.io.File
import java.io.FileNotFoundException

import scala.collection.Map
import scala.concurrent.duration._

import akka.actor._
import akka.actor.SupervisorStrategy.Escalate
import akka.actor.SupervisorStrategy.Restart
import akka.actor.SupervisorStrategy.Resume
import akka.actor.SupervisorStrategy.Stop
import akka.event.LogSource
import akka.event.LoggingReceive

import common.config.Configured
import model.Evaluated
import model.EvaluationState
import model.Image
import model.InEvaluation
import model.UnEvaluated
import util.ConfigTrait

class DirectoryActor extends Actor with ActorLogging with Configured {

  import CommonMsg._
  import DirectoryActor._
  import ListUtils._

  private[this] val appConfig = configured[ConfigTrait]

  private[this] var imageActors: Map[Image, ActorRef] = Map.empty

  var images = appConfig.images

  /*
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: UnsupportedOperationException ⇒ Resume
      case _: FileNotFoundException ⇒ Restart
      case _: ActorKilledException ⇒ Stop
      case _: Exception ⇒ Escalate
    }
*/

  override def preStart() {
    super.preStart
    log.debug("""
        starting DirectoryActor
        """)
  }

  override def postStop() {
    super.postStop
    log.debug("""
        DirectoryActor about to be stopped
        """)
  }

  private def findPairForImage(imageId: String): Option[(Image, ActorRef)] = {
    imageActors.find { case (img, actor) => img.id == imageId }
  }

  def receive : Receive = LoggingReceive {

    case RequestImage =>
      images.nextImage() match {
        // found an image to process
        case Some(image) =>
          // create a new ImageActor - it will ping itself after a timeout,
          // e.g. when no evaluation happened
          val (newImage, newImageList) = images.changeState(image, InEvaluation)
          images = newImageList
          val imageActor: ActorRef = context.actorOf(ImageActor.props(image), ImageActor.name(image))
          imageActors = imageActors + (newImage -> imageActor)

          sender ! Some(newImage)
        
        // we could not find an image to process
        case None =>
          log.debug("Nada: no image available")
          sender ! None
      }

    case StatusRequest =>
      sender ! images.computeStats()

    case ExpiredImageEvaluation(image) =>
      log.info(s"""
    		  Image $image expired, removing image from queue
    		  
      """)
      log.info(s"""
    		  ImageActors before : ${imageActors.size}
      """)
      imageActors = imageActors - image
      log.info(s"""
    		  ImageActors after : ${imageActors.size}
      """)
      val (newImage, img) = images.changeState(image, UnEvaluated)
      images = img
      val originalSender = sender
      context.stop(originalSender)

    case eval: Evaluation =>
      val imageName = new File(eval.id).getName()
      log.info(s"""
          reveiced evaluation for ${eval.id} / ${imageName}
          
          """)

      findPairForImage(imageName) match {
        // we found the image in the queue
        case Some((image, imageActor)) =>
          // forward the evaluation to the ServerActor
          log.info(s"""forwarding evaluation to ${imageActor}
          			corresponding image: ${image}
          			evaluation: ${eval}
          			""")
          val (newImage, newImages) = images.changeState(image.copy(tags = Some(eval.tags)), Evaluated)
          images = newImages
          log.info(s"""
              new image in list: ${newImage}
          """)
          imageActor forward eval

        case None =>
          log.info(s"""
              Evaluating image ${eval.id} has expired.
              
          """)
          sender ! EvaluationRejected(s"Image ${eval.id} has expired")
          val (newImage, img) = images.changeState(Image(eval.id, UnEvaluated), UnEvaluated)
          images = img
      }

    case SuccessfulImageEvaluation(image) =>
      val (newImage, newImages) = images.changeState(image, Evaluated)
      images = newImages
      val imageActor = imageActors(newImage)
      imageActors = imageActors - newImage
      context.stop(imageActor)

    case msg @ "failure" =>
      log.info(s"""
          received message: ${msg}.
      """)
      throw new UnsupportedOperationException()

    case msg @ "error" =>
      log.info(s"""
          received message: ${msg}.
      """)
      self ! akka.actor.Kill
  }
}

object DirectoryActor {
  case class Evaluation(id: String, tags: List[String])

  trait EvaluationStatus
  case object EvaluationAccepted extends EvaluationStatus
  case class EvaluationRejected(reason: String) extends EvaluationStatus

  case object RequestImage

  case object StatusRequest
  case class StatusResponse(total: Int,
    notEvaluated: Int,
    inEvaluation: Int,
    evaluated: Int,
    images: List[Image])

  def props() = Props[DirectoryActor]
  def name() = "DirectoryActor"

}

object ListUtils {
  implicit class StateChange(imageList: List[Image]) {
    def changeState(img: Image, newState: EvaluationState): (Image, List[Image]) = {
      val imageInList = imageList.find(_ == img.id).getOrElse(img)
      val newImage = imageInList.copy(state = newState)
      val newImageList = newImage :: imageList.filterNot(_.id == img.id)
      (newImage, newImageList)
    }

    /**
     *  @return the first image which remains to be evaluated if any.
     */
    def nextImage(): Option[Image] = {
      imageList.find(img => img.state == UnEvaluated)
    }

    def filterWith(state: EvaluationState) = imageList.filter(_.state == state)

    def computeStats(): DirectoryActor.StatusResponse =
      DirectoryActor.StatusResponse(imageList.size,
        imageList.filterWith(UnEvaluated).size,
        imageList.filterWith(InEvaluation).size,
        imageList.filterWith(Evaluated).size,
        imageList)
  }
}


