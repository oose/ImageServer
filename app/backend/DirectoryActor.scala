package backend

import java.io.File
import java.io.FileNotFoundException

import scala.collection.JavaConversions._
import scala.collection.Map
import scala.concurrent.duration.DurationInt

import org.apache.commons.io.FileUtils

import akka.actor._
import akka.actor.SupervisorStrategy.Escalate
import akka.actor.SupervisorStrategy.Restart
import akka.actor.SupervisorStrategy.Resume
import akka.actor.SupervisorStrategy.Stop
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive

import CommonMsg.ExpiredImageEvaluation
import common.config.Configured
import util.ConfigTrait

class DirectoryActor extends Actor with ActorLogging with Configured {

  import CommonMsg._
  import DirectoryActor._
  import ListUtils._

  private[this] val appConfig = configured[ConfigTrait]
  
  private[this] var imageActors: Map[Image, ActorRef] = Map.empty
  
  var images = appConfig.images

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: UnsupportedOperationException ⇒ Resume
      case _: FileNotFoundException ⇒ Restart
      case _: ActorKilledException ⇒ Stop
      case _: Exception ⇒ Escalate
    }

  override def preStart() {
    log.info("""
        starting DirectoryActor
        """)
  }

  override def postStop() {
    log.info("""
        DirectoryActor about to be stopped
        """)
  }


  private def findPairForImage(imageId: String): Option[(Image, ActorRef)] = {
    imageActors.find { case (img, actor) => img.id == imageId }
  }

  def receive = LoggingReceive {
    case RequestImage =>
      images.nextImage() match {
        // found an image to proccess
        case Some(image) =>
          // create a new ImageActor - it will ping itself after a timeout,
          // e.g. when no evaluation happened
          val imageActor = context.actorOf(Props(ImageActor(image)), s"ImageActor${image.id}")
          imageActors = imageActors + (image -> imageActor)
          images = images.changeState(image, InEvaluation)
          sender ! Some(image)
        // we could not find an image to process
        case None =>
          sender ! None
      }

    case StatusRequest =>
      sender ! images.computeStats()

    case ExpiredImageEvaluation(image) =>
      log.info(s"""
    		  Image $image expired, removing image from queue
    		  
      """)
      context.stop(sender)
      imageActors = imageActors - image
      images = images.changeState(image, UnEvaluated)

    // TODO refactor
    case eval: Evaluation =>
      val imageName = new File(eval.id).getName()
      log.info(s"""
          reveiced evaluation for ${eval.id} / ${imageName}.
          Images in queue: ${imageActors.keys}
          """)

      findPairForImage(imageName) match {
        case Some((image, actor)) =>
          // forward the evaluation to the ServerActor
          actor forward eval
          images = images.changeState(image.copy(tags = Some(eval.tags)), Evaluated)
        case None =>
          log.info(s"""
              Evaluating image ${eval.id} has expired.
              
          """)
          sender ! EvaluationRejected(s"Image ${eval.id} has expired")
          images = images.changeState(Image(eval.id, UnEvaluated), UnEvaluated)
      }

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

object ListUtils {
  implicit class StateChange(imageList: List[Image]) {
    def changeState(img: Image, newState: EvaluationState) = {
      img.copy(state = newState) :: imageList.filterNot(_.id == img.id)
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

/**
 * Enumeration of states an image can obtain.
 */
sealed trait EvaluationState
case object UnEvaluated extends EvaluationState
case object InEvaluation extends EvaluationState
case object Evaluated extends EvaluationState

case class Image(id: String, state: EvaluationState = UnEvaluated, tags: Option[List[String]] = None) {
  override def equals(arg: Any) = arg match {
    case Image(id, _, _) => id == this.id
    case _ => false
  }
  override def hashCode() = id.hashCode
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

}