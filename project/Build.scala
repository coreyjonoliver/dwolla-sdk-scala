import sbt.Keys._
import sbt._

object Build extends Build {

  import BuildSettings._
  import Dependencies._

  lazy val buildSettings = Seq(
    organization := "com.dwolla",
    version := "0.1",
    scalaVersion := "2.10.3"
  )
  lazy val root = Project(id = "root",
    base = file(".")) aggregate(dwollaSdk, examples)

  lazy val dwollaSdk = Project(id = "dwolla-sdk", base = file("dwolla-sdk"))
    .settings(dwollaModuleSettings: _*)
    .settings(libraryDependencies ++=
    provided(sprayClient, sprayJson, akkaActor, grizzledSlf4j)
    )

  lazy val examples = Project(id = "examples", base = file("examples"))
    .dependsOn(Seq(dwollaSdk))
    .settings(dwollaModuleSettings: _*)
    .settings(libraryDependencies ++=
    provided(sprayClient, sprayJson, akkaActor, grizzledSlf4j)
    )
}