package io.github.boykush.eff.addon.skunk.dbSessionIO

import cats.effect.IO
import io.github.boykush.eff.dbio.dbSessionIO.DBSessionIOEffect
import io.github.boykush.eff.dbio.dbSessionIO.DBSessionIOEffect._
import org.atnos.eff.Eff
import skunk.Session

trait SkunkDBSessionIOEffect {
  def withDBSession[R: _dbSessionIO, A](f: Session[IO] => IO[A]): Eff[R, A] = {
    DBSessionIOEffect.withDBSession[R, Session[IO], A](
      SkunkDBSession.sessionAsk(f)
    )
  }
}

object SkunkDBSessionIOEffect extends SkunkDBSessionIOEffect
