package io.github.boykush.eff.dbio.dbQueryIO

import cats.effect.IO
import org.atnos.eff.all.ThrowableEither
import org.atnos.eff.Fx
import org.atnos.eff.|=

trait DBQueryIOTypes {
  type DBQueryIOStack = Fx.fx3[DBQueryIO, IO, ThrowableEither]
  type _dbQueryIO[R]  = DBQueryIO |= R
}

object DBQueryIOTypes extends DBQueryIOTypes
