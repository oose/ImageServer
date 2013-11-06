package backend

import akka.actor._
import akka.camel._

import common.config.Configured
import util.AppConfig

/**
 * Camel Producer endpoint which is configured via [[util.AppConfig.camelEndpoint]].
 */
class CamelActor extends Actor with Producer with Oneway with Configured with ActorLogging{

  lazy val appConfig = configured[AppConfig]
  
  
      
  def endpointUri = appConfig.camelEndpoint
  
  override def preStart = {
    log.info(s"""
      CamelActor endpoint = ${appConfig.camelEndpoint} created
      """)
      
    log.debug(s"""
        CamelActor ${self} started.
        
    """)
  }
  override def postStop = {
    log.debug(s"""
        CamelActor ${self} stopped. 
    
    """)
  }
  
}