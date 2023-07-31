package io.github.boykush.eff.dbio.dbReadIO.interpreter

import cats.effect.IO
import io.github.boykush.eff.dbio.dbReadIO._
import org.atnos.eff._
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.all._

abstract class DBReadIOInterpreter {
  def run[R, U, A](effects: Eff[R, A], pager: Pager)(implicit
    m1: Member.Aux[DBReadIO, R, U],
    m2: Member.Aux[ThrowableEither, U, Fx.fx1[IO]],
    io: _io[U],
    te: _throwableEither[U]
  ): Eff[U, A]
}
