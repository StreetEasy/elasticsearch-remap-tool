import sbt._

object ElasticsearchRemapToolPlugins extends Build {

  lazy val plugins = Project(
    id = "elasticsearch-remap-tool-plugins",
    base = file(".")
  )
  .settings(
    addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.1")
  )

}
