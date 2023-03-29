package io.github.boykush.eff.syntax.addon.skunk

import io.github.boykush.eff.addon.skunk.dbSessionIO.interpreter.SkunkDBSessionIOInterpreter
import io.github.boykush.eff.dbio.dbSessionIO.DBSessionIO
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.Eff
import org.atnos.eff.Member
import org.atnos.eff.all._throwableEither

trait dbSessionIO {
  implicit final class ToSkunkDBSessionIOOps[R, A](e: Eff[R, A]) {
    def runDBSessionIO[U](implicit
      m: Member.Aux[DBSessionIO, R, U],
      io: _io[U],
      either: _throwableEither[U],
      int: SkunkDBSessionIOInterpreter
    ): Eff[U, A] =
      int.run[R, U, A](e)
  }
}

object dbSessionIO extends dbSessionIO
