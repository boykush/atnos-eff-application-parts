package io.github.boykush.eff.dbio.dbTransactionIO

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession

sealed trait DBTransactionIO[+A]

abstract class WithDBSession[S: DBSession, T] extends DBTransactionIO[T] {
  val f: S => IO[T]
}
