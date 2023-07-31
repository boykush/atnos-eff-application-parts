package io.github.boykush.eff.syntax.addon.skunk

import cats.effect.IO
import io.github.boykush.eff.addon.skunk.dbReadIO.interpreter.SkunkDBReadIOInterpreter
import io.github.boykush.eff.dbio.dbReadIO.DBReadIO
import io.github.boykush.eff.dbio.dbReadIO.Pager
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.Eff
import org.atnos.eff.Fx
import org.atnos.eff.Member
import org.atnos.eff.all.ThrowableEither

trait dbReadIO {
  implicit final class ToSkunkDBReadIOOps[R, A](e: Eff[R, A]) {
    def runDBReadIO[U](implicit
      m1: Member.Aux[DBReadIO, R, U],
      m2: Member.Aux[ThrowableEither, U, Fx.fx1[IO]],
      io: _io[U],
      int: SkunkDBReadIOInterpreter
    ): Eff[U, A] =
      int.run[R, U, A](e, Pager.all)

    def runDBReadIOWithPager[U](pager: Pager)(implicit
      m1: Member.Aux[DBReadIO, R, U],
      m2: Member.Aux[ThrowableEither, U, Fx.fx1[IO]],
      io: _io[U],
      int: SkunkDBReadIOInterpreter
    ): Eff[U, A] =
      int.run[R, U, A](e, pager)
  }
}

object dbReadIO extends dbReadIO
