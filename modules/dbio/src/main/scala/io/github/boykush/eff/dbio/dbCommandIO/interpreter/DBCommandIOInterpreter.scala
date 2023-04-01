package io.github.boykush.eff.dbio.dbCommandIO.interpreter

import io.github.boykush.eff.dbio.dbCommandIO.DBCommandIO
import org.atnos.eff._
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.all._

abstract class DBCommandIOInterpreter {
  def run[R, U, A](effects: Eff[R, A])(implicit
    m: Member.Aux[DBCommandIO, R, U],
    io: _io[U],
    either: _throwableEither[U]
  ): Eff[U, A]
}
