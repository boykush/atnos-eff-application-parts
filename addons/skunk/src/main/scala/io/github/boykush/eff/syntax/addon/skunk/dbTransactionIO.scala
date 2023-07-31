package io.github.boykush.eff.syntax.addon.skunk

import cats.effect.IO
import io.github.boykush.eff.addon.skunk.dbTransactionIO.interpreter.SkunkDBTransactionIOInterpreter
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIO
import org.atnos.eff.Eff
import org.atnos.eff.Fx
import org.atnos.eff.Member
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.all.ThrowableEither
import org.atnos.eff.all._throwableEither

trait dbTransactionIO {
  implicit final class ToSkunkDBTransactionIOOps[R, A](e: Eff[R, A]) {
    def runDBTransactionIO[U](implicit
      m1: Member.Aux[DBTransactionIO, R, U],
      m2: Member.Aux[ThrowableEither, U, Fx.fx1[IO]],
      io: _io[U],
      either: _throwableEither[U],
      int: SkunkDBTransactionIOInterpreter
    ): Eff[U, A] =
      int.run[R, U, A](e)
  }
}

object dbTransactionIO extends dbTransactionIO
