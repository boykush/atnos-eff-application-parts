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
  hogeModules,
).flatten

lazy val hogeModules: Seq[ProjectReference] = Seq(
  hogeLib,
)

lazy val hogeLib = (project in file("modules/hoge/lib"))
  .settings(Common.commonSettings)
