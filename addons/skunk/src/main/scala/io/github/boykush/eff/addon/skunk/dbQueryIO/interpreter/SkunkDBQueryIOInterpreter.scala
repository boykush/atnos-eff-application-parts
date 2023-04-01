package io.github.boykush.eff.addon.skunk.dbQueryIO.interpreter

import com.google.inject.Inject
import cats.effect._
import cats.effect.kernel.Resource
import io.github.boykush.eff.addon.skunk.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.SkunkDBSession
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIO
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIOError
import io.github.boykush.eff.dbio.dbQueryIO.WithDBSession
import io.github.boykush.eff.dbio.dbQueryIO.interpreter.DBQueryIOInterpreter
import natchez.Trace.Implicits.noop
import org.atnos.eff.Interpret._
import org.atnos.eff._
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.addon.cats.effect.IOEffect.fromIO
import org.atnos.eff.all._
import skunk._
import skunk.data.TransactionAccessMode
import skunk.data.TransactionIsolationLevel

class SkunkDBQueryIOInterpreter @Inject() (
  config: SkunkDBConfig,
  isolationLevel: TransactionIsolationLevel = TransactionIsolationLevel.RepeatableRead
) extends DBQueryIOInterpreter {
  def run[R, U, A](effects: Eff[R, A])(implicit
    m: Member.Aux[DBQueryIO, R, U],
    io: _io[U],
    te: _throwableEither[U]
  ): Eff[U, A] = translate(effects)(new Translate[DBQueryIO, U] {
    override def apply[X](sessionIO: DBQueryIO[X]): Eff[U, X] =
      sessionIO match {
        case WithDBSession(f: (SkunkDBSession => IO[X])) => fromIO[U, ThrowableEither[X]] {
            dbResource.use { pooled =>
              pooled.use { session =>
                session
                  .transaction(
                    isolationLevel = isolationLevel,
                    accessMode = TransactionAccessMode.ReadOnly
                  )
                  .use { _ =>
                    f(SkunkDBSession(session)).attempt
                  }
              }
            }
          }.flatMap(either =>
            fromEither[U, Throwable, X](
              either.left.map(e => DBQueryIOError(e.getMessage))
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
