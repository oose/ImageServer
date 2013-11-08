package backend

import scala.concurrent.duration.DurationInt

import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.JsValue
import play.api.libs.json.Json

import akka.actor._
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive

import DirectoryActor.StatusRequest
import util.Implicits.statusReponseJson

class StatusReportActor(directoryActor: ActorRef) extends Actor with ActorLogging {
  import StatusReportActor._
  import DirectoryActor._

  implicit val ec = context.dispatcher

  var children = List.empty[ActorRef]

  // setup scheduler to send a RequestStatus every 5 seconds
  context.system.scheduler.schedule(5 seconds, 5 seconds, self, StatusRequest)

  def receive = LoggingReceive {
    case RequestWebSocket =>
      val statusChild = context.actorOf(Props(StatusReportChildActor(directoryActor)))
      children = statusChild :: children
      statusChild forward RequestWebSocket

    case StatusRequest =>
      // TODO retrieve status from directory actor and 
      // push the result to all children
      children foreach (_ ! StatusRequest)

    case Quit =>
      context.stop(sender)
      children = children.filterNot(_ == sender)
  }
}

case class StatusReportChildActor(directoryActor: ActorRef) extends Actor with ActorLogging {
  import StatusReportActor._
  import DirectoryActor._

  implicit val ec = context.dispatcher

  val (out, outChannel) = Concurrent.broadcast[JsValue]

  // This handles any messages sent from the browser to the server over the socket
  val in = Iteratee.foreach[JsValue] { message =>
    // just take the socket data and send it as an akka message to our parent
    context.parent ! message
  }.map { _ =>
    // tell the parent we've quit
    context.parent ! Quit
  }

  def receive : Receive =  {
    case RequestWebSocket => sender ! WebSocketResponse(in, out)

    case StatusRequest =>
      directoryActor ! DirectoryActor.StatusRequest

    case sr: DirectoryActor.StatusResponse => 
      log.info(s"""
          pushing statusreponse to websocket channel.
          
          """)
      outChannel.push(Json.toJson(sr))
  }
}

object StatusReportActor {
  case object RequestWebSocket
  case class WebSocketResponse(in: Iteratee[JsValue, _], out: Enumerator[JsValue])
  case object Quit
  
  def props(directoryActor: ActorRef) = Props(classOf[StatusReportActor], directoryActor)
}