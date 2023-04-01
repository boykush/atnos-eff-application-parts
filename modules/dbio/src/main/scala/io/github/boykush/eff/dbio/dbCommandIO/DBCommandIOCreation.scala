package io.github.boykush.eff.dbio.dbCommandIO

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession
import io.github.boykush.eff.dbio.dbCommandIO.DBCommandIOTypes._dbCommandIO
import org.atnos.eff._

trait DBCommandIOCreation {
  def withDBSession[R: _dbCommandIO, S, A](f: DBSession[S] => IO[A]): Eff[R, A] =
    Eff.send[DBCommandIO, R, A](WithDBSession[S, A](f))
}

object DBCommandIOCreation extends DBCommandIOCreation
