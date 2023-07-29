package io.github.boykush.eff.dbio

import cats.effect.IO

trait DBSessionF[T] { self =>
  def execute[S <: DBSession](session: S): IO[T]
}
