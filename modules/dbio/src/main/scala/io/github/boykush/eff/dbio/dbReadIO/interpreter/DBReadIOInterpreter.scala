package io.github.boykush.eff.dbio.dbReadIO.interpreter

import io.github.boykush.eff.dbio.dbReadIO._
import org.atnos.eff._
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.all._

abstract class DBReadIOInterpreter {
  def run[R, U, A](effects: Eff[R, A], pager: Pager)(implicit
    m: Member.Aux[DBReadIO, R, U],
    io: _io[U],
    either: _throwableEither[U]
  ): Eff[U, A]
}
