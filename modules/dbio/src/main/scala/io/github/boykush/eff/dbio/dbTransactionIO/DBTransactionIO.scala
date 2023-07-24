package io.github.boykush.eff.dbio.dbTransactionIO

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession

sealed trait DBTransactionIO[+A]

case class WithDBSession[S, T](f: DBSession[S] => IO[T]) extends DBTransactionIO[T]
