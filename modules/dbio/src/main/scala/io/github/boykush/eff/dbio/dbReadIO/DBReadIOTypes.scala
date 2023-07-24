package io.github.boykush.eff.dbio.dbReadIO

import cats.effect.IO
import org.atnos.eff.all.ThrowableEither
import org.atnos.eff.Fx
import org.atnos.eff.|=

trait DBReadIOTypes {
  type DBReadIOStack = Fx.fx3[DBReadIO, IO, ThrowableEither]
  type _dbReadIO[R]  = DBReadIO |= R
}

object DBReadIOTypes extends DBReadIOTypes
