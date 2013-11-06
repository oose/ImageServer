import com.typesafe.sbt.SbtAtmosPlay.atmosPlaySettings

import play.Project._

name := "ImageServer"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.2"

libraryDependencies ++=  Seq(
    "commons-io" % "commons-io" % "2.4",
    "com.typesafe.akka" %% "akka-camel" % "2.2.0",
    "org.apache.activemq" % "activemq-camel" % "5.8.0",
    "org.webjars" %% "webjars-play" % "2.2.0",
    "org.webjars" % "angularjs" % "1.2.0-rc.3",
    "org.webjars" % "bootstrap" % "2.3.2",
    "com.typesafe.akka" %% "akka-testkit" % "2.2.0" % "test",
    "org.specs2" % "classycle" % "1.4.1" % "test",
    "org.scala-lang" % "scala-compiler" % "2.10.2" % "test"
)
    
playScalaSettings

atmosPlaySettings
 
lazy val ImageCommon = RootProject(file("../ImageCommon/"))

val ImageServer = project.in(file("."))
    .aggregate(ImageCommon).dependsOn(ImageCommon)