package io.github.boykush.eff.dbio.dbTransactionIO.interpreter

import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIO
import org.atnos.eff._
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.all._

abstract class DBTransactionIOInterpreter {
  def run[R, U, A](effects: Eff[R, A])(implicit
    m: Member.Aux[DBTransactionIO, R, U],
    io: _io[U],
    either: _throwableEither[U]
  ): Eff[U, A]
}
