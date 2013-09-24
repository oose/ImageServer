package backend

import scala.concurrent.duration._

import akka.actor._

class ImageActor(id: String) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  val ticker = context.system.scheduler.scheduleOnce(3.minutes, self, Expired)

  override def postStop = {
    log.info(s"ImageActor $id stopped.")
  }

  def receive = {

    case Evaluation(_, tags) => 
      ticker.cancel
      log.info(s"received tags $tags for actor $self")
      sender ! EvaluationAccepted

    case Expired =>
      log.info("Image Expired")
      context.parent ! Expired(Some(id))

    case _ => log.error("unknown message received")
  }

}
