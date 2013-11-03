package backend

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import org.specs2.time.NoTimeConversions
import akka.actor._
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import common.config.Configuration
import util.ConfigTrait
import scala.concurrent.duration._

/**
 *  A tiny class that can be used as a Specs2 'context'.
 */
abstract class AkkaTestkitSpecs2Support extends TestKit(ActorSystem())
  with After
  with ImplicitSender {
  
  // make sure we shut down the actor system after all tests have run
  def after = system.shutdown()
}

@RunWith(classOf[JUnitRunner])
class DirectoryActorSpec extends Specification with NoTimeConversions with Configuration {

  sequential

  implicit def stringToImage(name: String): Image = Image(name)
  val images: List[Image] = List("I1", "I2", "I3")
  
  configure {
    new ConfigTrait {

      override val imageDir = Some("/tmp")
      override val camelEndpoint = "direct:endpoint"
      override def images = images
    }
  }

  "DirectoryActor" should {
    import DirectoryActor._
    "respond to an image request" in new AkkaTestkitSpecs2Support {

      val directoryActor = system.actorOf(Props[DirectoryActor])
      directoryActor ! RequestImage
      expectMsg(Some(Image("I1")))
    }

    "respond to a StatusRequest" in new AkkaTestkitSpecs2Support {
      val directoryActor = system.actorOf(Props[DirectoryActor])
      directoryActor ! StatusRequest
      expectMsg(StatusResponse(3, 3, 0, 0, images))
    }

    "respond with None if repeatedly asked for an image" in new AkkaTestkitSpecs2Support {
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

