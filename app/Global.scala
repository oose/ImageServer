

import org.apache.activemq.camel.component.ActiveMQComponent

import play.api._

import akka.camel._

import common.config.Configuration
import util.AppConfig

object Global extends GlobalSettings with Configuration {

  override def onStart(app: Application) {
    val appConfig = new AppConfig()
    Logger.info(s"""
          Starting ImageServer with 
          dir: ${appConfig.imageDir} and
          endpoint: ${appConfig.camelEndpoint}
          
          """)
    configure {
      appConfig
    }
    Logger.info(s"""
          configure ActiveMQ component for Akka...
          
    	  ${Console.RED}Hint:
          start activemq : activemq start
          Admin console: open http://localhost:8161/admin${Console.RESET}
        """)
    val actorSystem = play.api.libs.concurrent.Akka.system(app)
    val camelContext = CamelExtension(actorSystem)
    val amqUrl = "tcp://localhost:61616"
    camelContext.context.addComponent("activemq", ActiveMQComponent.activeMQComponent(amqUrl))

  }
}