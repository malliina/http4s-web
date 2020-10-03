package com.malliina.web

import cats.data.NonEmptyList
import cats.effect._
import com.malliina.web.AppImplicits._
import com.malliina.web.AppService.pong
import io.circe.syntax._
import org.http4s._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object AppService {
  val pong = "pong"

  def apply(ctx: ContextShift[IO]): AppService = {
    val ec = ExecutionContext.global
    val http = HttpClient(ec, ctx)
    apply(http, ec, ctx)
  }

  def apply(http: HttpClient, ec: ExecutionContext, ctx: ContextShift[IO]): AppService = {
    val db = MyDatabase(ec, ctx)
    new AppService(http, db)
  }
}

class AppService(http: HttpClient, data: MyDatabase) {
//  implicit def enc[T: Encoder] = jsonEncoderOf[IO, T]

  val service = HttpRoutes.of[IO] {
    case GET -> Root / "ping"   => Ok(pong)
    case GET -> Root / "health" => Ok(AppMeta.meta.asJson)
    case req                    => NotFound(Errors(s"Not found: ${req.method} ${req.uri}.").asJson)
  }

  object parsers {
    def idQueryDecoder[T](build: String => T): QueryParamDecoder[T] =
      QueryParamDecoder.stringQueryParamDecoder.map(build)

    def parseOrDefault[T](q: Query, key: String, default: => T)(implicit dec: QueryParamDecoder[T]) =
      parseOpt[T](q, key).getOrElse(Right(default))

    def parse[T](q: Query, key: String)(implicit dec: QueryParamDecoder[T]) =
      parseOpt[T](q, key).getOrElse(Left(NonEmptyList(parseFailure(s"Query key not found: '$key'."), Nil)))

    def parseOpt[T](q: Query, key: String)(implicit dec: QueryParamDecoder[T]) =
      q.params.get(key).map { g =>
        dec.decode(QueryParameterValue(g)).toEither
      }
  }

  def parseFailure(message: String) = ParseFailure(message, message)
}

object AppServer extends IOApp {
  val app = Router("/" -> AppService(contextShift).service).orNotFound
  val server = BlazeServerBuilder[IO](ExecutionContext.global).bindHttp(port = 9000, "0.0.0.0").withHttpApp(app)

  override def run(args: List[String]): IO[ExitCode] = server.serve.compile.drain.as(ExitCode.Success)
}
