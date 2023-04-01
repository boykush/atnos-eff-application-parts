package io.github.boykush.eff.dbio.dbCommandIO

import cats.effect.IO
import org.atnos.eff.all.ThrowableEither
import org.atnos.eff.Fx
import org.atnos.eff.|=

trait DBCommandIOTypes {
  type DBCommandIOStack = Fx.fx3[DBCommandIO, IO, ThrowableEither]
  type _dbCommandIO[R]  = DBCommandIO |= R
}

object DBCommandIOTypes extends DBCommandIOTypes
