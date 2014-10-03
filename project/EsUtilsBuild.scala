import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._
import com.gu.SbtDistPlugin.{distSettings,distFiles}
import com.github.siasia.WebPlugin.{webSettings, container}
import com.github.siasia.PluginKeys.port

object EsUtilsBuild extends Build {

  lazy val project = Project(
    id = "es-utils",
    base = file("."),
    settings = Defaults.defaultSettings ++ distSettings ++ assemblySettings ++ Seq(
      organization := "com.gu.contentapi",
      scalaVersion := "2.10.0",
      jarName in assembly := "es-utils.jar",
      distFiles <++= (sourceDirectory in Compile) map { src => (src / "deploy" ***) x flat },
      distFiles <+= (assembly in Compile) map { _ -> "packages/es-utils/es-utils.jar" },
      libraryDependencies ++= Seq(
        "org.json4s"   %% "json4s-jackson" % "3.1.0",
        "org.elasticsearch" % "elasticsearch" % "1.1.1",
        "com.spatial4j" % "spatial4j" % "0.3",
        "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
        "ch.qos.logback" % "logback-classic" % "1.0.9"
      ),
      mergeStrategy in assembly <<= (mergeStrategy in assembly) {
        (old) => {
          case "about.html" => MergeStrategy.first
          case x => old(x)
        }
      }
    )
  )
}
