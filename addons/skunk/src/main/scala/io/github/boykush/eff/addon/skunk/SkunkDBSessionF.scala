package io.github.boykush.eff.addon.skunk

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession
import io.github.boykush.eff.dbio.DBSessionF
import skunk.Session

class SkunkDBSessionF[A](f: Session[IO] => IO[A]) extends DBSessionF[A] {
  override def execute[S <: DBSession](session: S): IO[A] =
    session match {
      case s: SkunkDBSession => f.apply(s.v)
      case s                 =>
        IO.raiseError(new InternalError(s"Illegal implement caused by receive $s DBSession"))
    }
}
