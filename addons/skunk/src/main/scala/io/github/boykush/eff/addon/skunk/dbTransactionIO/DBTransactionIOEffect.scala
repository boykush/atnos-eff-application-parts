package io.github.boykush.eff.addon.skunk.dbTransactionIO

import cats.effect.IO
import io.github.boykush.eff.addon.skunk.SkunkDBSessionF
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIO
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIOError
import io.github.boykush.eff.dbio.dbTransactionIO.WithDBSession
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIOTypes._dbTransactionIO
import org.atnos.eff.Eff
import org.atnos.eff.all._
import skunk.Session

trait DBTransactionIOEffect {
  def resolveWithLock[R: _dbTransactionIO: _throwableEither, A](
    f: Session[IO] => IO[Option[A]]
  ): Eff[R, A] = {
    Eff
      .send[DBTransactionIO, R, Option[A]](
        WithDBSession[Option[A]](
          new SkunkDBSessionF[Option[A]](f)
        )
      )
      .flatMap(maybe =>
        fromEither[R, Throwable, A](
          maybe.toRight(DBTransactionIOError.ResourceNotFoundError)
        )
      )
  }

  def store[R: _dbTransactionIO](f: Session[IO] => IO[Unit]): Eff[R, Unit] =
    Eff
      .send[DBTransactionIO, R, Unit](
        WithDBSession[Unit](
          new SkunkDBSessionF[Unit](f)
        )
      )
}

object DBTransactionIOEffect extends DBTransactionIOEffect
