import com.typesafe.sbt.packager.docker.DockerVersion
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.buildInfoKeys
import scala.sys.process.Process
import scala.util.Try

val http4sVersion = "0.21.7"
val circeVersion = "0.13.0"
val prodPort = 9000

inThisBuild(
  Seq(
    organization := "com.malliina",
    version := "0.0.1",
    scalaVersion := "2.13.3"
  )
)

val client = project
  .in(file("client"))
  .enablePlugins(ScalaJSBundlerPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    npmDependencies in Compile ++= Seq(
      )
    //crossTarget in (Compile, packageJSDependencies) := (resourceManaged in Compile).value,
  )

val server = project
  .in(file("."))
  .enablePlugins(JavaServerAppPackaging, BuildInfoPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-scalatags" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-play-json" % http4sVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.slf4j" % "slf4j-api" % "1.7.30",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "ch.qos.logback" % "logback-core" % "1.2.3",
      "org.scalameta" %% "munit" % "0.7.12" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    // https://github.com/ChristopherDavenport/http4s-scalajsexample/blob/master/build.sbt
    resources in Compile += (fastOptJS in (client, Compile)).value.data,
    resources in Compile += (fastOptJS in (client, Compile)).value
      .map((x: sbt.File) => new File(x.getAbsolutePath + ".map"))
      .data,
    //(managedResources in Compile) += (artifactPath in (client, Compile, packageJSDependencies)).value,
    watchSources ++= (watchSources in client).value,
    buildInfoPackage := "com.malliina.web",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, "gitHash" -> gitHash)
  )

val root = project.in(file("root")).aggregate(client, server)

def gitHash: String =
  sys.env
    .get("GITHUB_SHA")
    .orElse(Try(Process("git rev-parse HEAD").lineStream.head).toOption)
    .getOrElse("unknown")

bloopExportJarClassifiers in Global := Some(Set("sources"))
