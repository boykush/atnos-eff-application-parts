package io.github.boykush.eff.dbio.dbTransactionIO.interpreter

import cats.effect.IO
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIO
import org.atnos.eff._
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.all._

abstract class DBTransactionIOInterpreter {
  def run[R, U, A](effects: Eff[R, A])(implicit
    m1: Member.Aux[DBTransactionIO, R, U],
    m2: Member.Aux[ThrowableEither, U, Fx.fx1[IO]],
    io: _io[U],
    either: _throwableEither[U]
  ): Eff[U, A]
}
