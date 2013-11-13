package backend

import scala.concurrent.duration.DurationInt

import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import akka.actor._
import akka.testkit._

import DirectoryActor.RequestImage
import DirectoryActor.StatusRequest
import DirectoryActor.StatusResponse
import model.Image
import oose.play.config.Configuration
import oose.play.akka.test.AkkaSpecs2Scope
import util.ConfigTrait

class DirectoryActorSpec extends SpecificationWithJUnit with NoTimeConversions with Configuration {

  sequential

  implicit def stringToImage(name: String): Image = Image(name)
  val imageList: List[Image] = List("I1", "I2", "I3")
  
  configure {
    new ConfigTrait {

      override val imageDir = Some("/tmp")
      override val camelEndpoint = "direct:endpoint"
      override val images = imageList
      override val imageEvaluationTimeOut = 5 seconds
    }
  }

  "DirectoryActor" should {
    import DirectoryActor._
    "respond to an image request" in new AkkaSpecs2Scope {

      val directoryActor = system.actorOf(Props[DirectoryActor])
      directoryActor ! RequestImage
      expectMsg(Some(Image("I1")))
    }

    "respond to a StatusRequest" in new AkkaSpecs2Scope {
      val directoryActor = system.actorOf(Props[DirectoryActor])
      directoryActor ! StatusRequest
      expectMsg(StatusResponse(3, 3, 0, 0, imageList))
    }

    "respond with None if repeatedly asked for an image" in new AkkaSpecs2Scope {
      val directoryActor = system.actorOf(Props[DirectoryActor])
      directoryActor ! RequestImage
      fishForMessage(5.seconds, "Trying to request images") {
        case Some(Image(_, _, _)) =>
          directoryActor ! RequestImage
          false
        case None => true
      }
    }
  }
}

