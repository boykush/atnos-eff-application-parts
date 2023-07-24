package io.github.boykush.eff.addon.skunk.dbTransactionIO

import cats.effect.IO
import io.github.boykush.eff.addon.skunk.SkunkDBSession
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIOEffect
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIOTypes._dbTransactionIO
import org.atnos.eff.Eff
import skunk.Session

trait SkunkDBTransactionIOEffect {
  def withDBSession[R: _dbTransactionIO, A](f: Session[IO] => IO[A]): Eff[R, A] = {
    DBTransactionIOEffect.withDBSession[R, Session[IO], A](
      SkunkDBSession.sessionAsk(f)
    )
  }
}

object SkunkDBTransactionIOEffect extends SkunkDBTransactionIOEffect
