import com.softwaremill.SbtSoftwareMillCommon.commonSmlBuildSettings
import Dependencies._

val scala2 = List("2.13.15")
val scala3 = List("3.3.4")

def dependenciesFor(version: String)(deps: (Option[(Long, Long)] => ModuleID)*): Seq[ModuleID] =
  deps.map(_.apply(CrossVersion.partialVersion(version)))

lazy val commonSettings = commonSmlBuildSettings ++ Seq(
  version := "0.2.3-BL1",
  organization := "com.softwaremill.sttp.openai",
  gitPublishDir := file("/src/maven-repo"),
  licenses += License.Apache2,
)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    publish / skip := true,
    gitRelease := {},
    name := "sttp-openai",
    scalaVersion := scala2.head,
  )
  .aggregate(allAgregates: _*)

lazy val allAgregates = core.projectRefs ++
  fs2.projectRefs ++
  zio.projectRefs ++
  pekko.projectRefs ++
  akka.projectRefs ++
  ox.projectRefs ++
  examples.projectRefs ++
  docs.projectRefs

lazy val core = (projectMatrix in file("core"))
  .jvmPlatform(
    scalaVersions = scala2 ++ scala3
  )
  .settings(
    libraryDependencies ++= Seq(
      Libraries.tapirApispecDocs,
      Libraries.uJsonCirce,
      Libraries.uPickle
    ) ++ Libraries.sttpApispec ++ Libraries.sttpClient ++ Seq(Libraries.scalaTest)
  )
  .settings(commonSettings: _*)

lazy val fs2 = (projectMatrix in file("streaming/fs2"))
  .jvmPlatform(
    scalaVersions = scala2 ++ scala3
  )
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Libraries.sttpClientFs2
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val zio = (projectMatrix in file("streaming/zio"))
  .jvmPlatform(
    scalaVersions = scala2 ++ scala3
  )
  .settings(commonSettings)
  .settings(
    libraryDependencies += Libraries.sttpClientZio
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val pekko = (projectMatrix in file("streaming/pekko"))
  .jvmPlatform(
    scalaVersions = scala2 ++ scala3
  )
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Libraries.sttpClientPekko
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val akka = (projectMatrix in file("streaming/akka"))
  .jvmPlatform(
    scalaVersions = scala2
  )
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Libraries.sttpClientAkka
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val ox = (projectMatrix in file("streaming/ox"))
  .jvmPlatform(
    scalaVersions = scala3
  )
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Libraries.sttpClientOx
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val examples = (projectMatrix in file("examples"))
  .jvmPlatform(
    scalaVersions = scala3
  )
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-netty-server-sync" % "1.11.7",
      "ch.qos.logback" % "logback-classic" % "1.5.6"
    ) ++ Libraries.sttpClientOx,
    publish / skip := true,
    gitRelease := {},
  )
  .dependsOn(ox)

val compileDocs: TaskKey[Unit] = taskKey[Unit]("Compiles docs module throwing away its output")
compileDocs := {
  (docs.jvm(scala2.head) / mdoc).toTask(" --out target/sttp-openai-docs").value
}

lazy val docs = (projectMatrix in file("generated-docs")) // important: it must not be docs/
  .enablePlugins(MdocPlugin)
  .settings(commonSettings)
  .settings(
    mdocIn := file("README.md"),
    moduleName := "sttp-openai-docs",
    mdocOut := file("generated-docs/README.md"),
    mdocExtraArguments := Seq("--clean-target"),
    publishArtifact := false,
    gitRelease := {},
    name := "docs",
    evictionErrorLevel := Level.Info
  )
  .dependsOn(core, fs2, zio, akka, pekko)
  .jvmPlatform(scalaVersions = scala2)
