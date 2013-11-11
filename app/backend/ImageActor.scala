package backend

import java.io.File
import java.io.InputStream
import scala.collection.immutable.List
import scala.collection.immutable.Map
import org.apache.camel.Exchange
import org.apache.commons.io.IOUtils
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import akka.actor._
import akka.camel._
import akka.event.LoggingReceive
import oose.play.config.Configured
import model.Image
import _root_.util.ConfigTrait
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util._
import model.Evaluated

case class ImageActor(image: Image) extends Actor with ActorLogging with Configured {
  import DirectoryActor._

  implicit val ec = context.dispatcher
  
  val appConfig = configured[ConfigTrait]

  val camelActor : ActorRef = {
    val ref = context.actorOf(CamelActor.props, "CamelActor")
    ref
  }

  val ticker = context.system.scheduler.scheduleOnce(appConfig.imageEvaluationTimeOut, self, ImageActor.TimeOutImageEvaluation)
  
  implicit def jsonToInputStream(js: JsValue) : InputStream = { 
    IOUtils.toInputStream(Json.prettyPrint(js))
  }

  override def preStart = {
    super.preStart
    log.info(s"""
        ImageActor $image started.
        
    """)
  }
  override def postStop = {
    super.postStop
    log.info(s"""
        ImageActor $image stopped. 
    
    """)
  }

  private def extractFileName(id: String) : String = {
    new File(id).getName()
  }

  def receive : Receive = LoggingReceive {

    case eval @ DirectoryActor.Evaluation(id, tags) =>
      import ImageActor._
      import util.Implicits._  
      
      log.info(s"""
          received tags $tags 
          for image $id 
          on actor $self
          
      """)
      
      ticker.cancel
      
      // send Json as Inputstream to CamelActor
      val is  : InputStream = Json.toJson(eval)
      val fileName = extractFileName(id)
      log.debug(s"""
          sending tags as json to CamelActor with fileName : ${fileName}
      
      """)
      val headerMap = Map(Exchange.FILE_NAME -> fileName)
      camelActor ! CamelMessage(body = is, headers = headerMap) 
      context.parent ! CommonMsg.SuccessfulImageEvaluation(image.copy(state = Evaluated, tags = Some(tags))) // send back to DirectoryActor
      sender ! DirectoryActor.EvaluationAccepted // forwarded from Application controller
 
    case ImageActor.TimeOutImageEvaluation =>
      log.info(s"""
          Image $image expired, received time out from scheduler
          sending parent an ExpiredImageEvaluation message
          
          
          Will canceling the ticker have an effect: ${ticker.cancel}
      """)
      context.parent ! CommonMsg.ExpiredImageEvaluation(image)
  }

}

object ImageActor {
  object TimeOutImageEvaluation
  case class Evaluation(id: String, tags: List[String])
  
  def props(id: Image) = Props(classOf[ImageActor], id)
  def name(image: Image) = s"ImageActor${image.id}"
}
