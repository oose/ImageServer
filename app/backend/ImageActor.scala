package backend

import java.io.File

import scala.collection.immutable.List
import scala.collection.immutable.Map
import scala.concurrent.duration.DurationInt

import org.apache.camel.Exchange
import org.apache.commons.io.IOUtils

import play.api.libs.json.Json

import akka.actor._
import akka.actor.actorRef2Scala
import akka.camel._
import akka.event.LoggingReceive

import util.Implicits.evaluationJson

case class ImageActor(id: Image) extends Actor with ActorLogging {
  import DirectoryActor._

  implicit val ec = context.dispatcher

  val camelActor = context.actorOf(Props[CamelActor], "CamelActor")

  val ticker = context.system.scheduler.scheduleOnce(3.minutes, self, ImageActor.TimeOutImageEvaluation)

  override def preStart = {
    log.info(s"""
        ImageActor $id started.
        
    """)
  }
  override def postStop = {
    log.info(s"""
        ImageActor $id stopped. 
    """)
  }

  private def extractFileName(id: String) = {
    new File(id).getName()
  }

  def receive = LoggingReceive {

    case eval @ DirectoryActor.Evaluation(id, tags) =>
      import ImageActor._
      ticker.cancel
      log.info(s"""
          received tags $tags / image $id for actor $self
          
      """)
      sender ! DirectoryActor.EvaluationAccepted
      
      // send Json as Inputstream to CamelActor
      val is = IOUtils.toInputStream(Json.prettyPrint(Json.toJson(eval)))
      val headerMap = Map(Exchange.FILE_NAME -> extractFileName(id))
      camelActor ! CamelMessage(body = is, headers = headerMap)

    case ImageActor.TimeOutImageEvaluation =>
      log.info(s"""
          Image $id expired, received time out from scheduler
          sending parent an ExpiredImageEvaluation message
          
      """)
      context.parent ! CommonMsg.ExpiredImageEvaluation(id)
  }

}

object ImageActor {
  object TimeOutImageEvaluation
  case class Evaluation(id: String, tags: List[String])
}
