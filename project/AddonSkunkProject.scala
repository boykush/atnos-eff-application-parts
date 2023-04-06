import sbt.Keys._
import sbt._

object AddonSkunkProject {
  lazy val settings = Seq(
    libraryDependencies ++= Seq(
      "org.tpolecat"   %% "skunk-core"  % "0.5.1",
      "net.codingwell" %% "scala-guice" % "5.1.1",
      "com.typesafe"    % "config"      % "1.4.2"
    )
  )
}
