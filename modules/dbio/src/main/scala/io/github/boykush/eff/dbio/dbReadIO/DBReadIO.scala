package io.github.boykush.eff.dbio.dbReadIO

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession

sealed trait DBReadIO[+A]

abstract class WithDBSession[S: DBSession, T] extends DBReadIO[T] {
  val f: S => IO[T]
}
