package io.github.boykush.eff.dbio.dbReadIO

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession

sealed trait DBReadIO[+A]

case class WithDBSession[S, T](f: DBSession[S] => IO[T]) extends DBReadIO[T]
