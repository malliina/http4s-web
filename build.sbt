import java.nio.file.Path
import java.nio.file.{Files, StandardCopyOption}

import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.buildInfoKeys

import scala.sys.process.Process
import scala.util.Try

val http4sVersion = "0.21.7"
val circeVersion = "0.13.0"
val prodPort = 9000

val assetsDir = settingKey[Path]("Webpack assets dir to serve in server")
val prepTarget = taskKey[Path]("Prep target dir")

inThisBuild(
  Seq(
    organization := "com.malliina",
    version := "0.0.1",
    scalaVersion := "2.13.3",
    assetsDir := (baseDirectory.value / "assets").toPath
  )
)

val client = project
  .in(file("client"))
  .enablePlugins(ScalaJSBundlerPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.9.1"
    ),
    scalaJSUseMainModuleInitializer := true,
    version in webpack := "4.44.2",
    version in startWebpackDevServer := "3.7.2",
    webpackConfigFile in fastOptJS := Some(baseDirectory.value / "webpack.dev.config.js"),
    webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack.prod.config.js"),
    webpackBundlingMode in (Compile, fastOptJS) := BundlingMode.LibraryOnly(),
    webpackBundlingMode in (Compile, fullOptJS) := BundlingMode.Application,
    webpackEmitSourceMaps := false,
    npmDevDependencies in Compile ++= Seq(
      "autoprefixer" -> "10.0.1",
      "cssnano" -> "4.1.10",
      "css-loader" -> "4.3.0",
      "file-loader" -> "6.1.0",
      "less" -> "3.12.2",
      "less-loader" -> "7.0.1",
      "mini-css-extract-plugin" -> "0.11.3",
      "postcss" -> "8.0.5",
      "postcss-import" -> "12.0.1",
      "postcss-loader" -> "4.0.3",
      "postcss-preset-env" -> "6.7.0",
      "style-loader" -> "1.3.0",
      "url-loader" -> "4.1.0",
      "webpack-merge" -> "5.1.4"
    ),
    webpack.in(Compile, fastOptJS) := {
      val files = webpack.in(Compile, fastOptJS).value
      val log = streams.value.log
      files.foreach { file =>
        val relativeFile = file.data.relativeTo(crossTarget.in(Compile, npmUpdate).value).get
        val dest = assetsDir.value.resolve(relativeFile.toPath)
        Files.createDirectories(dest.getParent)
        Files.copy(file.data.toPath, dest, StandardCopyOption.REPLACE_EXISTING)
        log.info(s"Copied ${file.data} of type ${file.metadata.get(BundlerFileTypeAttr)} to '$dest'.")
      }
      files
    },
    webpack.in(Compile, fastOptJS) := webpack.in(Compile, fastOptJS).dependsOn(prepTarget).value,
    prepTarget := {
      Files.createDirectories(assetsDir.value)
    }
  )

val server = project
  .in(file("."))
  .enablePlugins(JavaServerAppPackaging, BuildInfoPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.malliina" %% "primitives" % "1.17.0",
      "com.lihaoyi" %% "scalatags" % "0.9.1",
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
    resources in Compile ++= webpack.in(client, Compile, fastOptJS).value.map(_.data),
    reStart := reStart.dependsOn(webpack.in(client, Compile, fastOptJS)).evaluated,
    watchSources ++= (watchSources in client).value,
    buildInfoPackage := "com.malliina.web",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, "gitHash" -> gitHash),
    resourceDirectories in Compile += assetsDir.value.toFile
  )

val root = project.in(file("root")).aggregate(client, server)

def gitHash: String =
  sys.env
    .get("GITHUB_SHA")
    .orElse(Try(Process("git rev-parse HEAD").lineStream.head).toOption)
    .getOrElse("unknown")
