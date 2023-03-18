import sbt._
import Keys._

object Common {
  lazy val commonSettings = Seq(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.15" % "test",
    ),
    scalacOptions += "-Wunused:imports"
  )
}