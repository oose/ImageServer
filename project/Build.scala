import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "ImageServer"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
   "com.typesafe.akka" %% "akka-camel" % "2.1.0",
   "commons-io" % "commons-io" % "2.4"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
