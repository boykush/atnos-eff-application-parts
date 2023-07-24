package io.github.boykush.eff.syntax.addon.skunk

import io.github.boykush.eff.addon.skunk.dbTransactionIO.interpreter.SkunkDBTransactionIOInterpreter
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIO
import org.atnos.eff.Eff
import org.atnos.eff.Member
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.all._throwableEither

trait dbTransactionIO {
  implicit final class ToSkunkDBTransactionIOOps[R, A](e: Eff[R, A]) {
    def runDBTransactionIO[U](implicit
      m: Member.Aux[DBTransactionIO, R, U],
      io: _io[U],
      either: _throwableEither[U],
      int: SkunkDBTransactionIOInterpreter
    ): Eff[U, A] =
      int.run[R, U, A](e)
  }
}

object dbTransactionIO extends dbTransactionIO
