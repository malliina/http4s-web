package com.malliina.web

import java.nio.file.Paths

import cats.data._
import cats.effect._
import cats.implicits._
import com.malliina.values.UnixPath
import com.malliina.web.AppImplicits._
import com.malliina.web.AppService.{log, pong}
import com.malliina.web.html.{Assets, Html}
import io.circe.syntax._
import org.http4s.CacheDirective._
import org.http4s._
import org.http4s.headers._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.staticcontent._
import org.http4s.server.{Router, Server}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import AppService.assets

object AppService {
  private val log = LoggerFactory.getLogger(getClass)
  val pong = "pong"
  val assets = Assets(Paths.get("assets"))
  def apply(blocker: Blocker, ctx: ContextShift[IO]): AppService = {
    val ec = ExecutionContext.global
    val http = HttpClient(ec, ctx)
    apply(http, ec, blocker, ctx)
  }

  def apply(http: HttpClient, ec: ExecutionContext, blocker: Blocker, ctx: ContextShift[IO]): AppService = {
    val db = MyDatabase(ec, ctx)
    new AppService(http, db, ec, blocker)(ctx)
  }
}

class AppService(http: HttpClient, data: MyDatabase, ec: ExecutionContext, blocker: Blocker)(
  implicit cs: ContextShift[IO]
) {
  val supportedStaticExtensions =
    List(".html", ".js", ".map", ".css", ".png", ".ico")
  val html = Html(isProd = false)
  val service = HttpRoutes.of[IO] {
    case GET -> Root            => Ok(html.index.tags)
    case GET -> Root / "ping"   => Ok(pong)
    case GET -> Root / "health" => Ok(AppMeta.meta.asJson)
    case req @ GET -> "assets" /: rest if supportedStaticExtensions.exists(req.pathInfo.endsWith) =>
      val file = UnixPath(rest.toList.mkString("/"))
      val isCacheable = file.value.count(_ == '.') == 2
      val cacheHeaders =
        if (isCacheable) NonEmptyList.of(`max-age`(365.days), `public`)
        else NonEmptyList.of(`no-cache`())
      // Reads from the assets dir, alternatively from resources (alternative is not necessary as it stands)
      StaticFile
        .fromFile(Paths.get("assets").resolve(file.value).toFile, blocker, Option(req))
        .map(_.putHeaders(`Cache-Control`(cacheHeaders)))
        .orElse(StaticFile.fromResource[IO](file.value, blocker, req.some))
        .fold(notFound(req))(_.pure[IO])
        .flatten
    case req => notFound(req)
  }
  val router = Router(
    "/" -> service,
    "files" -> fileService(FileService.Config[IO]("./files", blocker))
  )

  def notFound(req: Request[IO]) = {
    val error = Errors(s"Not found: ${req.method} ${req.uri}.")
    log.info(error.message)
    NotFound(error.asJson)
  }
}

object AppServer extends IOApp {
  val app: Resource[IO, Server[IO]] = for {
    blocker <- Blocker[IO]
    server <- BlazeServerBuilder[IO](ExecutionContext.global)
      .bindHttp(port = 9000, "0.0.0.0")
      .withHttpApp(AppService(blocker, contextShift).router.orNotFound)
      .resource
  } yield server

  override def run(args: List[String]): IO[ExitCode] = app.use(_ => IO.never).as(ExitCode.Success)
}
