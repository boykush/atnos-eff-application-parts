package io.github.boykush.eff.addon.skunk.dbReadIO

import cats.effect.IO
import io.github.boykush.eff.addon.skunk.SkunkDBSession
import io.github.boykush.eff.dbio.dbReadIO.DBReadIOEffect
import io.github.boykush.eff.dbio.dbReadIO.DBReadIOEffect._
import org.atnos.eff.Eff
import skunk.Session

trait SkunkDBReadIOEffect {
  def withDBSession[R: _dbReadIO, A](f: Session[IO] => IO[A]): Eff[R, A] = {
    DBReadIOEffect.withDBSession[R, Session[IO], A](
      SkunkDBSession.sessionAsk(f)
    )
  }
}

object SkunkDBReadIOEffect extends SkunkDBReadIOEffect
