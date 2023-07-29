package io.github.boykush.eff.addon.skunk

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession
import skunk.Session

case class SkunkDBSession(v: Session[IO]) extends DBSession
