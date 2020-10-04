package com.malliina.web

import cats.data.NonEmptyList
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

case class Name(name: String) extends AnyVal

case class UserId(id: String) extends AnyVal

case class Person(name: String, age: Int)

object Person {
  implicit val json = deriveCodec[Person]
}

case class AppResult(message: String)

object AppResult {
  val example = AppResult("The result!")
  implicit val json = deriveCodec[AppResult]
}

case class AppMeta(version: String, gitHash: String)

object AppMeta {
  implicit val json = deriveCodec[AppMeta]
  val meta = AppMeta(BuildInfo.version, BuildInfo.gitHash)
}

case class SingleError(message: String, key: String)

object SingleError {
  implicit val json = deriveCodec[SingleError]
  def input(message: String): SingleError = apply(message, "input")
}

case class Errors(errors: NonEmptyList[SingleError]) {
  def message = errors.head.message
}

object Errors {
  implicit val json = deriveCodec[Errors]
  def apply(message: String): Errors = Errors(NonEmptyList(SingleError(message, "general"), Nil))
}

trait PrimitiveId extends Any {
  def id: String
  override def toString = id
}

trait PrimitiveCompanion[T <: PrimitiveId] {
  def apply(s: String): T
  implicit val dec = Decoder.decodeString.map[T](s => apply(s))
  implicit val enc = Encoder.encodeString.contramap[T](_.id)
}
