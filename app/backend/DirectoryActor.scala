package backend

import java.io.File
import scala.collection.JavaConversions.collectionAsScalaIterable
import org.apache.commons.io.FileUtils
import akka.actor._
import akka.event.LoggingReceive
import java.net.URI
import scala.collection._

class DirectoryActor(imageDir: String) extends Actor with ActorLogging {

  import CommonMsg._
  import DirectoryActor._
  import ListUtils._

  private[this] var images: List[Image] = scanDirectory(imageDir)

  private[this] var imageActors: Map[Image, ActorRef] = Map.empty

  /**
   * @return a list of graphic files in the directory.
   */
  def scanDirectory(dir: String): List[Image] = {
    log.info("dir: " + dir)
    val files = FileUtils.listFiles(new File(dir), Array("jpg", "png", "gif"), false)
    files.map(f => Image(f.getName())).toList
  }

  def receive = LoggingReceive {
    case RequestImageId =>
      val image = images.nextImage()
      image match {
        case Some(image) =>
          // create a new ImageActor - it will ping itself after a timeout,
          // e.g. when no evaluation happened
          val imageActor = context.actorOf(Props(new ImageActor(image, self)), s"ImageActor${image.id}")
          imageActors = imageActors + (image -> imageActor)
          images = images.changeState(image, InEvaluation)
        case None =>
      }
      // return the Option[String] to the sender
      sender ! image

    case StatusRequest =>
      sender ! images.computeStats()

    case ExpiredImageEvaluation(image) =>
      log.info(s"""
    		  Image $image expired, removing image from queue
    		  
      """)
      context.stop(sender)
      imageActors = imageActors - image
      images = images.changeState(image, UnEvaluated)

    case eval : Evaluation =>
      val imageName = new File(eval.id).getName()
      log.info(s"""
          reveiced evaluation for ${eval.id} / ${imageName}.
          Images in queue: ${imageActors.keys}
          """)

      // TODO improve method
      val imgActor = imageActors.find { case (img, actor) => img.id == imageName }

      imgActor match {
        case Some((image, actor)) =>
          actor forward eval
          images = images.changeState(image.copy(tags = Some(eval.tags)), Evaluated)
        case None =>
          log.info(s"""
              Evaluating image ${eval.id} has expired.
              
          """)
          sender ! EvaluationRejected(s"Image ${eval.id} has expired")
          images = images.changeState(Image(eval.id, UnEvaluated), UnEvaluated)
      }
  }

}

object ListUtils {
  implicit class StateChange(list: List[Image]) {
    def changeState(img: Image, newState: EvaluationState) = {
      img.copy(state = newState) :: list.filterNot(_.id == img.id)
    }

    /**
     *  @return the first image which remains to be evaluated if any.
     */
    def nextImage(): Option[Image] = {
      list.find(img => img.state == UnEvaluated)
    }

    def filterWith(state: EvaluationState) = list.filter(_.state == state)
    
    def computeStats() = DirectoryActor.StatusResponse(list.size,
        list.filterWith(UnEvaluated).map(_.id),
        list.filterWith(InEvaluation).map(_.id),
        list.filterWith(Evaluated).map(_.id))
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

  case object RequestImageId

  case object StatusRequest
  case class StatusResponse(total: Int, unevaluated: List[String], inEvaluation: List[String], evaluated: List[String])
}