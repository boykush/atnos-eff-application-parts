package io.github.boykush.eff.addon.skunk.dbCommandIO

import cats.effect.IO
import io.github.boykush.eff.addon.skunk.SkunkDBSession
import io.github.boykush.eff.dbio.dbCommandIO.DBCommandIOEffect
import io.github.boykush.eff.dbio.dbCommandIO.DBCommandIOTypes._dbCommandIO
import org.atnos.eff.Eff
import skunk.Session

trait SkunkDBCommandIOEffect {
  def withDBSession[R: _dbCommandIO, A](f: Session[IO] => IO[A]): Eff[R, A] = {
    DBCommandIOEffect.withDBSession[R, Session[IO], A](
      SkunkDBSession.sessionAsk(f)
    )
  }
}

object SkunkDBCommandIOEffect extends SkunkDBCommandIOEffect
