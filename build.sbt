name         := baseDirectory.value.name
organization := "io.github.boykush"

inThisBuild(
  List(
    scalaVersion      := "2.13.10",
    semanticdbEnabled := true, // enable SemanticDB
    semanticdbVersion := "4.7.5"
  )
)

lazy val root = (project in file("."))
  .aggregate(allProjects: _*)
  .dependsOn(allProjects.map(ClasspathDependency(_, None)): _*)

lazy val allProjects: Seq[ProjectReference] = Seq(
  dbio,
  addonSkunk
)

lazy val dbio = (project in file("modules/dbio"))
  .settings(Common.commonSettings)
  .settings(DBIOProject.settings)

lazy val addonSkunk = (project in file("addons/skunk"))
  .settings(Common.commonSettings)
  .settings(AddonSkunkProject.settings)
  .dependsOn(dbio)
