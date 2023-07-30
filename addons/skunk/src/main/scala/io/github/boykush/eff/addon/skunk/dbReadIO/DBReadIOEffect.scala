package io.github.boykush.eff.addon.skunk.dbReadIO

import cats.effect.IO
import io.github.boykush.eff.addon.skunk.SkunkDBSessionF
import io.github.boykush.eff.dbio.dbReadIO.DBReadIO
import io.github.boykush.eff.dbio.dbReadIO.Pager
import io.github.boykush.eff.dbio.dbReadIO.WithDBSession
import io.github.boykush.eff.dbio.dbReadIO.WithDBSessionAndPager
import io.github.boykush.eff.dbio.dbReadIO.DBReadIOEffect._
import org.atnos.eff.Eff
import skunk.Session

trait DBReadIOEffect {
  def withDBSession[R: _dbReadIO, A](f: Session[IO] => IO[A]): Eff[R, A] = {
    Eff.send[DBReadIO, R, A](
      WithDBSession[A](
        new SkunkDBSessionF[A](f)
      )
    )
  }

  def withDBSessionAndPager[R: _dbReadIO, A](
    f: (Session[IO], Pager) => IO[List[A]]
  ): Eff[R, List[A]] =
    Eff.send[DBReadIO, R, List[A]](
      WithDBSessionAndPager[A](
        new SkunkDBSessionAndPagerF[A](f)
      )
    )
}

object DBReadIOEffect extends DBReadIOEffect
