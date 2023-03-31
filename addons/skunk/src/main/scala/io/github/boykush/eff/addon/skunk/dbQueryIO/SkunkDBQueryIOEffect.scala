package io.github.boykush.eff.addon.skunk.dbQueryIO

import cats.effect.IO
import io.github.boykush.eff.addon.skunk.SkunkDBSession
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIOEffect
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIOEffect._
import org.atnos.eff.Eff
import skunk.Session

trait SkunkDBQueryIOEffect {
  def withDBSession[R: _dbQueryIO, A](f: Session[IO] => IO[A]): Eff[R, A] = {
    DBQueryIOEffect.withDBSession[R, Session[IO], A](
      SkunkDBSession.sessionAsk(f)
    )
  }
}

object SkunkDBQueryIOEffect extends SkunkDBQueryIOEffect
