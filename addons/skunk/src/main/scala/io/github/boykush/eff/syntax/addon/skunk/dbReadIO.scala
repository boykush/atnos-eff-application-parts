package io.github.boykush.eff.syntax.addon.skunk

import io.github.boykush.eff.addon.skunk.dbReadIO.interpreter.SkunkDBReadIOInterpreter
import io.github.boykush.eff.dbio.dbReadIO.DBReadIO
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.Eff
import org.atnos.eff.Member
import org.atnos.eff.all._throwableEither

trait dbReadIO {
  implicit final class ToSkunkDBReadIOOps[R, A](e: Eff[R, A]) {
    def runDBReadIO[U](implicit
      m: Member.Aux[DBReadIO, R, U],
      io: _io[U],
      either: _throwableEither[U],
      int: SkunkDBReadIOInterpreter
    ): Eff[U, A] =
      int.run[R, U, A](e)
  }
}

object dbReadIO extends dbReadIO
