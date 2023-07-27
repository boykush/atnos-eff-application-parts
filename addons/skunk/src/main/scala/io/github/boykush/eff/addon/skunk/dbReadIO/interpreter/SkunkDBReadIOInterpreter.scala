package io.github.boykush.eff.addon.skunk.dbReadIO.interpreter

import com.google.inject.Inject
import cats.effect._
import cats.effect.kernel.Resource
import io.github.boykush.eff.addon.skunk.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.dbReadIO.SkunkDBReadIO.WithDBSession
import io.github.boykush.eff.dbio.dbReadIO.DBReadIO
import io.github.boykush.eff.dbio.dbReadIO.DBReadIOError
import io.github.boykush.eff.dbio.dbReadIO.interpreter.DBReadIOInterpreter
import natchez.Trace.Implicits.noop
import org.atnos.eff.Interpret._
import org.atnos.eff._
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.addon.cats.effect.IOEffect.fromIO
import org.atnos.eff.all._
import skunk._
import skunk.data.TransactionAccessMode
import skunk.data.TransactionIsolationLevel

class SkunkDBReadIOInterpreter @Inject() (
  config: SkunkDBConfig,
  isolationLevel: TransactionIsolationLevel = TransactionIsolationLevel.RepeatableRead
) extends DBReadIOInterpreter {
  def run[R, U, A](effects: Eff[R, A])(implicit
    m: Member.Aux[DBReadIO, R, U],
    io: _io[U],
    te: _throwableEither[U]
  ): Eff[U, A] = translate(effects)(new Translate[DBReadIO, U] {
    override def apply[X](readIO: DBReadIO[X]): Eff[U, X] =
      readIO match {
        case WithDBSession(f: (Session[IO] => IO[X])) => fromIO[U, ThrowableEither[X]] {
            dbResource.use { pooled =>
              pooled.use { session =>
                session
                  .transaction(
                    isolationLevel = isolationLevel,
                    accessMode = TransactionAccessMode.ReadOnly
                  )
                  .use { _ =>
                    f(session).attempt
                  }
              }
            }
          }.flatMap(either =>
            fromEither[U, Throwable, X](
              either.left.map(e => DBReadIOError(e))
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
