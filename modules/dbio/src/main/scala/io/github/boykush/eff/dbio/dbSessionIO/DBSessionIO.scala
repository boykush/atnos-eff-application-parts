package io.github.boykush.eff.dbio.dbSessionIO

import cats.effect.IO

sealed trait DBSessionIO[+A]

case class WithDBSession[S, T](f: DBSession[S] => IO[T]) extends DBSessionIO[T]
