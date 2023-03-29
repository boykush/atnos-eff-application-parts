package io.github.boykush.eff.addon.skunk.dbSessionIO.interpreter

import com.google.inject.Inject
import cats.effect._
import cats.effect.kernel.Resource
import io.github.boykush.eff.addon.skunk.dbSessionIO.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.dbSessionIO.SkunkDBSession
import io.github.boykush.eff.dbio.DBIOError
import io.github.boykush.eff.dbio.dbSessionIO.DBSessionIO
import io.github.boykush.eff.dbio.dbSessionIO.WithDBSession
import io.github.boykush.eff.dbio.dbSessionIO.interpreter.DBSessionIOInterpreter
import natchez.Trace.Implicits.noop
import org.atnos.eff.Interpret._
import org.atnos.eff._
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.addon.cats.effect.IOEffect.fromIO
import org.atnos.eff.all._
import skunk._

class SkunkDBSessionIOInterpreter @Inject() (
  config: SkunkDBConfig
) extends DBSessionIOInterpreter {
  def run[R, U, A](effects: Eff[R, A])(implicit
    m: Member.Aux[DBSessionIO, R, U],
    io: _io[U],
    te: _throwableEither[U]
  ): Eff[U, A] = translate(effects)(new Translate[DBSessionIO, U] {
    override def apply[X](sessionIO: DBSessionIO[X]): Eff[U, X] =
      sessionIO match {
        case WithDBSession(f: (SkunkDBSession => IO[X])) => fromIO[U, ThrowableEither[X]] {
            dbResource.use { pooled =>
              pooled.use { session =>
                f(SkunkDBSession(session)).attempt
              }
            }
          }.flatMap(either =>
            fromEither[U, Throwable, X](
              either.left.map(e => DBIOError(e.getMessage))
            )
          )
      }
  })

  lazy val dbResource: Resource[IO, Resource[IO, Session[IO]]] = Session
    .pooled[IO](
      host = config.host,
      port = config.port,
      user = config.user,
      database = config.database,
      password = Some(config.password),
      max = config.maxConnections
    )
}
