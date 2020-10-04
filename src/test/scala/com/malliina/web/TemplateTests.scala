package com.malliina.web

import java.util.concurrent.Executors

import cats.effect._
import munit.FunSuite
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

class TemplateTests extends FunSuite {
  implicit val ec = ExecutionContext.global
  implicit val ctx = IO.contextShift(ec)
  val blocker = Blocker.liftExecutorService(Executors.newCachedThreadPool())
  val service = AppService(blocker, ctx).service.orNotFound

  test("can make request") {
    val pingRequest = Request[IO](Method.GET, uri"/ping")
    val response = service.run(pingRequest).unsafeRunSync()
    assertEquals(response.status, Status.Ok)
    val body = response.as[String].unsafeRunSync()
    assertEquals(body, AppService.pong)
  }
}
