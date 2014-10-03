import sbt._
import Keys._

object Plugins extends Build {
  lazy val plugins = Project("plugins", file("."))
    .dependsOn(
    uri("git://github.com/guardian/sbt-version-info-plugin.git#1.0"),
    uri("git://github.com/guardian/sbt-dist-plugin.git#1.6")
  ).settings(
    addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.6"),
    libraryDependencies <+= sbtVersion(v => v match {
      case x if (x.startsWith("0.12")) => "com.github.siasia" %% "xsbt-web-plugin" % "0.12.0-0.2.11.1"
    })
  )
}


