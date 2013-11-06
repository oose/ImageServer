package util

import java.io.File
import scala.collection.JavaConversions._
import org.apache.commons.io.FileUtils
import play.api._
import scala.concurrent.duration._
import model.Image

trait ConfigTrait {

  def imageDir: Option[String]
  def camelEndpoint: String
  def images: List[Image]
  def imageEvaluationTimeOut: FiniteDuration
}

class AppConfig extends ConfigTrait {
  
  val imageDir: Option[String] =
    Play.current.configuration.getString("image.dir")

  val camelEndpoint =
    Play.current.configuration.getString("camel.endpoint").getOrElse("none")

  
  val imageEvaluationTimeOut = timeOutValue("image.evaluation.timeout")

  
  val images = scanImageDirectory
  
  
   private def timeOutValue(key: String) : FiniteDuration = {
      val value = Play.current.configuration.getMilliseconds(key)
      value match {
        case Some(duration) => FiniteDuration(duration, MILLISECONDS)
        case None =>
          throw Play.current.configuration.globalError(s"Missing configuration key: [$key]")
      } 
   }
  
  /**
   * @return a list of graphic files in the directory.
   */
  private def scanImageDirectory(): List[Image] = {
    imageDir.flatMap { dir =>
      val files = FileUtils.listFiles(new File(dir), Array("jpg", "png", "gif"), false)
      Some(files.map(f => Image(f.getName())).toList)
    }.getOrElse(List.empty)
  }

}