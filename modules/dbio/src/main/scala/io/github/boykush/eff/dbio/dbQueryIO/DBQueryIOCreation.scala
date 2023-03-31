package io.github.boykush.eff.dbio.dbQueryIO

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIOTypes._dbQueryIO
import org.atnos.eff._

trait DBQueryIOCreation {
  def withDBSession[R: _dbQueryIO, S, A](f: DBSession[S] => IO[A]): Eff[R, A] =
    Eff.send[DBQueryIO, R, A](WithDBSession[S, A](f))
}

object DBQueryIOCreation extends DBQueryIOCreation
