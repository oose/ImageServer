package backend

import akka.actor._
import akka.camel._

import oose.play.config.Configured
import util.AppConfig

/**
 * Camel Producer endpoint which is configured via [[util.AppConfig.camelEndpoint]].
 */
class CamelActor extends Actor with Producer with Oneway with Configured with ActorLogging {

  val appConfig = configured[AppConfig]

  def endpointUri = appConfig.camelEndpoint

}

object CamelActor {
  def props() = Props[CamelActor]
}