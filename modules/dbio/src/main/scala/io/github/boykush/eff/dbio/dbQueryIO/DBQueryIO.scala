package io.github.boykush.eff.dbio.dbQueryIO

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession

sealed trait DBQueryIO[+A]

case class WithDBSession[S, T](f: DBSession[S] => IO[T]) extends DBQueryIO[T]
