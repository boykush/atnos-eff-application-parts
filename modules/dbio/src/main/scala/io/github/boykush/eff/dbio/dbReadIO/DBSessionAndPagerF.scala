package io.github.boykush.eff.dbio.dbReadIO

import cats.effect.IO
import io.github.boykush.eff.dbio.DBSession

trait DBSessionAndPagerF[T] { self =>
  def execute[S <: DBSession](session: S, pager: Pager): IO[List[T]]
}
