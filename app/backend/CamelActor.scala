package backend

import akka.actor.Actor
import akka.camel.Producer
import common.config.Configured
import util.AppConfig
import akka.actor.ActorLogging
import akka.camel.Oneway

/**
 * Camel Producer endpoint which is configured via [[util.AppConfig.camelEndpoint]].
 */
class CamelActor extends Actor with Producer with Oneway with Configured with ActorLogging{

  lazy val appConfig = configured[AppConfig]
  
  log.info(s"""
      Camel endpoint = ${appConfig.camelEndpoint}
      """)
      
  def endpointUri = appConfig.camelEndpoint
  
}