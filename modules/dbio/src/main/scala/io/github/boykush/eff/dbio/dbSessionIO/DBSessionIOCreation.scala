package io.github.boykush.eff.dbio.dbSessionIO

import cats.effect.IO
import io.github.boykush.eff.dbio.dbSessionIO.DBSessionIOTypes._dbSessionIO
import org.atnos.eff._

trait DBSessionIOCreation {
  def withDBSession[R: _dbSessionIO, S, A](f: DBSession[S] => IO[A]): Eff[R, A] =
    Eff.send[DBSessionIO, R, A](WithDBSession[S, A](f))
}

object DBSessionIOCreation extends DBSessionIOCreation
