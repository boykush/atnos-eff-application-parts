package io.github.boykush.eff.addon.skunk.dbReadIO

import cats.effect.IO
import io.github.boykush.eff.addon.skunk.SkunkDBSession
import io.github.boykush.eff.dbio.dbReadIO.DBSessionAndPagerF
import io.github.boykush.eff.dbio.dbReadIO.Pager
import io.github.boykush.eff.dbio.DBSession
import skunk.Session

class SkunkDBSessionAndPagerF[A](f: (Session[IO], Pager) => IO[List[A]])
    extends DBSessionAndPagerF[A] {
  override def execute[S <: DBSession](session: S, pager: Pager): IO[List[A]] =
    session match {
      case s: SkunkDBSession => f.apply(s.v, pager)
      case s                 =>
        IO.raiseError(new InternalError(s"Illegal implement caused by receive $s DBSession"))
    }
}
