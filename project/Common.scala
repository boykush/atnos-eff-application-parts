import sbt._
import Keys._

object Common {
  lazy val commonSettings = Seq(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.20" % "test",
      "org.atnos" %% "eff" % "7.0.6"
    ),
    scalacOptions += "-Wunused:imports"
  )
}