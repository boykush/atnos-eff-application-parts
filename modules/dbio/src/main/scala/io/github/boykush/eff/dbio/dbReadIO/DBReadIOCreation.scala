package io.github.boykush.eff.dbio.dbReadIO

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession
import io.github.boykush.eff.dbio.dbReadIO.DBReadIOTypes._dbReadIO
import org.atnos.eff._

trait DBReadIOCreation {
  def withDBSession[R: _dbReadIO, S, A](f: DBSession[S] => IO[A]): Eff[R, A] =
    Eff.send[DBReadIO, R, A](WithDBSession[S, A](f))
}

object DBReadIOCreation extends DBReadIOCreation
