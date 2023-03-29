package io.github.boykush.eff.dbio.dbSessionIO.interpreter

import io.github.boykush.eff.dbio.dbSessionIO.DBSessionIO
import org.atnos.eff._
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.all._

abstract class DBSessionIOInterpreter {
  def run[R, U, A](effects: Eff[R, A])(implicit
    m: Member.Aux[DBSessionIO, R, U],
    io: _io[U],
    either: _throwableEither[U]
  ): Eff[U, A]
}
