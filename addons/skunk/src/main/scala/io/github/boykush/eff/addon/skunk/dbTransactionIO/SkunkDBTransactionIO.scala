package io.github.boykush.eff.addon.skunk.dbTransactionIO

import cats.effect.IO
import io.github.boykush.eff.addon.skunk.SkunkDBSession
import io.github.boykush.eff.dbio.dbTransactionIO
import skunk.Session

object SkunkDBTransactionIO {

  import SkunkDBSession._

  case class WithDBSession[X](override val f: Session[IO] => IO[X])
      extends dbTransactionIO.WithDBSession[Session[IO], X]

}
