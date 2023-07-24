package io.github.boykush.eff.dbio.dbTransactionIO

import cats.effect.IO
import org.atnos.eff.all.ThrowableEither
import org.atnos.eff.Fx
import org.atnos.eff.|=

trait DBTransactionIOTypes {
  type DBTransactionIOStack = Fx.fx3[DBTransactionIO, IO, ThrowableEither]
  type _dbTransactionIO[R]  = DBTransactionIO |= R
}

object DBTransactionIOTypes extends DBTransactionIOTypes
