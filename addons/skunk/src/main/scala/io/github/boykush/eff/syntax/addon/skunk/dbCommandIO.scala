package io.github.boykush.eff.syntax.addon.skunk

import io.github.boykush.eff.addon.skunk.dbCommandIO.interpreter.SkunkDBCommandIOInterpreter
import io.github.boykush.eff.dbio.dbCommandIO.DBCommandIO
import org.atnos.eff.Eff
import org.atnos.eff.Member
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.all._throwableEither

trait dbCommandIO {
  implicit final class ToSkunkDBCommandIOOps[R, A](e: Eff[R, A]) {
    def runDBCommandIO[U](implicit
      m: Member.Aux[DBCommandIO, R, U],
      io: _io[U],
      either: _throwableEither[U],
      int: SkunkDBCommandIOInterpreter
    ): Eff[U, A] =
      int.run[R, U, A](e)
  }
}

object dbCommandIO extends dbCommandIO
