

import play.api._
import common.config.Configuration
import play.api.Logger
import util.AppConfig
import akka.camel.CamelExtension
import org.apache.activemq.camel.component.ActiveMQComponent

object Global extends GlobalSettings with Configuration {

  override def onStart(app: Application) {
    Logger.info("""
          Starting ImageServer...
          
          """)
    configure {
      new AppConfig()
    }
    Logger.info("""
          configure ActiveMQ component for Akka...
          
          """)
    val actorSystem = play.api.libs.concurrent.Akka.system(app)
    val camelContext = CamelExtension(actorSystem)
    val amqUrl = "tcp://localhost:61616"
    camelContext.context.addComponent("activemq", ActiveMQComponent.activeMQComponent(amqUrl))

  }
}