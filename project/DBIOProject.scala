import sbt.Keys._
import sbt._

object DBIOProject {
  lazy val settings = Seq(
    libraryDependencies ++= Seq(
      "org.atnos" %% "eff-cats-effect" % "7.0.6"
    )
  )
}
