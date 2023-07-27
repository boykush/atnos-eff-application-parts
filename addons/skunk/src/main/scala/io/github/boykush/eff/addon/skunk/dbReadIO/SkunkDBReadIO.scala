package io.github.boykush.eff.addon.skunk.dbReadIO

import cats.effect.IO
import io.github.boykush.eff.addon.skunk.SkunkDBSession
import io.github.boykush.eff.dbio.dbReadIO
import skunk.Session

object SkunkDBReadIO {
  import SkunkDBSession._
  case class WithDBSession[X](override val f: Session[IO] => IO[X])
      extends dbReadIO.WithDBSession[Session[IO], X]

}
