package io.github.boykush.eff.addon.skunk.dbTransactionIO

import cats.effect.IO
import io.github.boykush.eff.addon.skunk.SkunkDBSessionF
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIO
import io.github.boykush.eff.dbio.dbTransactionIO.WithDBSession
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIOTypes._dbTransactionIO
import org.atnos.eff.Eff
import skunk.Session

trait SkunkDBTransactionIOEffect {
  def withDBSession[R: _dbTransactionIO, A](f: Session[IO] => IO[A]): Eff[R, A] = {
    Eff.send[DBTransactionIO, R, A](
      WithDBSession[A](
        new SkunkDBSessionF[A](f)
      )
    )
  }
}

object SkunkDBTransactionIOEffect extends SkunkDBTransactionIOEffect
