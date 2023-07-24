package io.github.boykush.eff.dbio.dbTransactionIO

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIOTypes._dbTransactionIO
import org.atnos.eff._

trait DBTransactionIOCreation {
  def withDBSession[R: _dbTransactionIO, S, A](f: DBSession[S] => IO[A]): Eff[R, A] =
    Eff.send[DBTransactionIO, R, A](WithDBSession[S, A](f))
}

object DBTransactionIOCreation extends DBTransactionIOCreation
