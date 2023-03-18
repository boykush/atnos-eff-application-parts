name := baseDirectory.value.name
organization := "io.github.boykush"

inThisBuild(
  List(
    scalaVersion := "2.13.10",
    semanticdbEnabled := true, // enable SemanticDB
    semanticdbVersion := "4.7.5"
  )
)

lazy val root = (project in file("."))
  .aggregate(allModules: _*)
  .dependsOn(allModules.map(ClasspathDependency(_, None)): _*)

lazy val allModules: Seq[ProjectReference] = Seq(
  dbioModules,
).flatten

lazy val dbioModules: Seq[ProjectReference] = Seq(
  dbioProject,
)

lazy val dbioProject = (project in file("modules/dbio"))
  .settings(Common.commonSettings)
  .settings(DBIO.settings)
