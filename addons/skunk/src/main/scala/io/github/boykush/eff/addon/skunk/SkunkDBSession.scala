package io.github.boykush.eff.addon.skunk

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession
import skunk.Session

object SkunkDBSession {
  implicit val dbSessionOps: DBSession[Session[IO]] = new DBSession[Session[IO]] {}
}
