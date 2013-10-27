

import play.api._
import common.config.Configuration
import play.api.Logger
import util.AppConfig

object Global  extends GlobalSettings with Configuration {

  override def onStart(app: Application) {
    configure {
      Logger.info("""
          Starting ImageServer...
          
          """)
      new AppConfig()
    }
  }
}