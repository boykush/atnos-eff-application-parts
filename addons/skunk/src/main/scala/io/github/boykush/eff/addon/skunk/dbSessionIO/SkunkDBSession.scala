package io.github.boykush.eff.addon.skunk.dbSessionIO

import cats.effect.IO
import io.github.boykush.eff.dbio.dbSessionIO.DBSession
import skunk.Session

case class SkunkDBSession(value: Session[IO]) extends DBSession[Session[IO]]
