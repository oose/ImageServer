import sbt._
import Keys._
import play.Project._
import com.typesafe.sbt.SbtAtmosPlay.atmosPlaySettings

object ApplicationBuild extends Build {

  val appName = "ImageServer"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "commons-io" % "commons-io" % "2.4",
    "com.typesafe.akka" %% "akka-camel" % "2.2.0",
    "org.apache.activemq" % "activemq-camel" % "5.8.0",
    "org.webjars" %% "webjars-play" % "2.2.0",
    "org.webjars" % "angularjs" % "1.2.0-rc.3",
    "org.webjars" % "bootstrap" % "2.3.2",
    "com.typesafe.akka" %% "akka-testkit" % "2.2.0" % "test")

  scalacOptions ++= Opts.doc.title(s"$appName Documentation")
  scalacOptions ++= Seq("-feature")

  lazy val imageCommon = RootProject(file("../ImageCommon/"))

  val main = play.Project(appName, appVersion, appDependencies).settings( // Add your own project settings here      
  ).settings(atmosPlaySettings: _*)
    .aggregate(imageCommon).dependsOn(imageCommon)

}
