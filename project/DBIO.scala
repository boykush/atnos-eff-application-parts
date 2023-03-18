import sbt.Keys._
import sbt._

object DBIO {
  lazy val settings = Seq(
    libraryDependencies ++= Seq(
      "io.monix" %% "monix-eval" % "3.4.1"
    )
  )
}