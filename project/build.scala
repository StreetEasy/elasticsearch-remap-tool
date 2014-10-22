import sbt._
import sbt.Keys._
import sbtassembly.Plugin._
import sbtassembly.Plugin.AssemblyKeys._

object ElasticsearchRemapToolBuild extends Build {

  lazy val project = Project(
    id = "elasticsearch-remap-tool",
    base = file(".")
  )
  .settings(
    scalaVersion := "2.10.4",
    scalacOptions ++= Seq("-feature", "-deprecation"),
    libraryDependencies ++= Seq(
      "org.json4s"  %% "json4s-jackson" % "3.2.8",
      "org.elasticsearch" % "elasticsearch" % "1.1.1",
      "com.typesafe" %% "scalalogging-slf4j" % "1.1.0",
      "ch.qos.logback" % "logback-classic" % "1.1.2"
    )
  )
  .settings(assemblySettings: _*)

}
