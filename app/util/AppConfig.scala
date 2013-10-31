package util

import play.api._

class AppConfig {

  val imageDir: Option[String] = {
    Play.current.configuration.getString("image.dir")
  }

  val camelEndpoint =
    Play.current.configuration.getString("camel.endpoint").getOrElse("none")
}