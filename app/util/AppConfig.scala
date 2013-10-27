package util

import scala.collection.JavaConversions._
import play.api._
import scala.concurrent.duration._

class AppConfig {
  
   val camelEndpoint = 
       Play.current.configuration.getString("camel.endpoint").getOrElse("none")
}