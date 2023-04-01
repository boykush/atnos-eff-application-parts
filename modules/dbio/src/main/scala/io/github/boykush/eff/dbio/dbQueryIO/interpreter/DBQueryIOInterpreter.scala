package io.github.boykush.eff.dbio.dbQueryIO.interpreter

import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIO
import org.atnos.eff._
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.all._

abstract class DBQueryIOInterpreter {
  def run[R, U, A](effects: Eff[R, A])(implicit
    m: Member.Aux[DBQueryIO, R, U],
    io: _io[U],
    either: _throwableEither[U]
  ): Eff[U, A]
}
