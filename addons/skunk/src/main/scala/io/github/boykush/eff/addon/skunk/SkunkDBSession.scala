package io.github.boykush.eff.addon.skunk

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession
import skunk.Session

case class SkunkDBSession(value: Session[IO]) extends DBSession[Session[IO]]

object SkunkDBSession {
  def sessionAsk[A](f: (Session[IO] => IO[A])): DBSession[Session[IO]] => IO[A] =
    f.compose[DBSession[Session[IO]]](_.value)

}
