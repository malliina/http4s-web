package com.malliina.web

import cats.data.NonEmptyList
import org.http4s.{ParseFailure, Query, QueryParamDecoder, QueryParameterValue}

object Parsers {
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

  def parseFailure(message: String) = ParseFailure(message, message)
}
