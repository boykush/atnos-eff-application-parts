import sbt._
import Keys._

object Common {
  lazy val commonSettings = Seq(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.15" % "test",
      "org.atnos" %% "eff" % "6.0.1"
    ),
    scalacOptions += "-Wunused:imports"
  )
}