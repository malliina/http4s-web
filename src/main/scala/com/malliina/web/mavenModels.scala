package com.malliina.web

import io.circe.{Decoder, Encoder}

trait PrimitiveId extends Any {
  def id: String
  override def toString = id
}

trait PrimitiveCompanion[T <: PrimitiveId] {
  def apply(s: String): T
  implicit val dec = Decoder.decodeString.map[T](s => apply(s))
  implicit val enc = Encoder.encodeString.contramap[T](_.id)
}
