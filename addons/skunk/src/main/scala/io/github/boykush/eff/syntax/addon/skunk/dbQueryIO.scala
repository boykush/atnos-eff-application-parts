package io.github.boykush.eff.syntax.addon.skunk

import io.github.boykush.eff.addon.skunk.dbQueryIO.interpreter.SkunkDBQueryIOInterpreter
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIO
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.Eff
import org.atnos.eff.Member
import org.atnos.eff.all._throwableEither

trait dbQueryIO {
  implicit final class ToSkunkDBQueryIOOps[R, A](e: Eff[R, A]) {
    def runDBQueryIO[U](implicit
      m: Member.Aux[DBQueryIO, R, U],
      io: _io[U],
      either: _throwableEither[U],
      int: SkunkDBQueryIOInterpreter
    ): Eff[U, A] =
      int.run[R, U, A](e)
  }
}

object dbQueryIO extends dbQueryIO
