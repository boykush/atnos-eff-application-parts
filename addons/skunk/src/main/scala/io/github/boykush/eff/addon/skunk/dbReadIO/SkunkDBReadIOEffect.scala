package io.github.boykush.eff.addon.skunk.dbReadIO

import cats.effect.IO
import io.github.boykush.eff.dbio.dbReadIO.DBReadIO
import io.github.boykush.eff.dbio.dbReadIO.DBReadIOEffect._
import org.atnos.eff.Eff
import skunk.Session

trait SkunkDBReadIOEffect {
  def withDBSession[R: _dbReadIO, A](f: Session[IO] => IO[A]): Eff[R, A] = {
    Eff.send[DBReadIO, R, A](SkunkDBReadIO.WithDBSession[A](f))
  }
}

object SkunkDBReadIOEffect extends SkunkDBReadIOEffect
