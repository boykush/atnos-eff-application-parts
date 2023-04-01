package io.github.boykush.eff.dbio.dbCommandIO

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession

sealed trait DBCommandIO[+A]

case class WithDBSession[S, T](f: DBSession[S] => IO[T]) extends DBCommandIO[T]
