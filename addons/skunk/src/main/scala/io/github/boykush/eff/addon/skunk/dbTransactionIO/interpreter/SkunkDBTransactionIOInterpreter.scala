package io.github.boykush.eff.addon.skunk.dbTransactionIO.interpreter

import cats.effect._
import cats.effect.kernel.Resource
import cats.effect.unsafe.IORuntime
import com.google.inject.Inject
import io.github.boykush.eff.addon.skunk.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.SkunkDBSession
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIO
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIOError
import io.github.boykush.eff.dbio.dbTransactionIO.WithDBSession
import io.github.boykush.eff.dbio.dbTransactionIO.interpreter.DBTransactionIOInterpreter
import natchez.Trace.Implicits.noop
import org.atnos.eff.Interpret._
import org.atnos.eff._
import org.atnos.eff.addon.cats.effect.IOEffect._
import org.atnos.eff.all._
import org.atnos.eff.syntax.addon.cats.effect.toIOOps
import org.atnos.eff.syntax.all._
import skunk._
import skunk.data.TransactionAccessMode
import skunk.data.TransactionIsolationLevel

class SkunkDBTransactionIOInterpreter @Inject() (
  config: SkunkDBConfig,
  isolationLevel: TransactionIsolationLevel = TransactionIsolationLevel.RepeatableRead
) extends DBTransactionIOInterpreter {

  implicit val runtime: IORuntime = IORuntime.global

  def run[R, U, A](effects: Eff[R, A])(implicit
    m1: Member.Aux[DBTransactionIO, R, U],
    m2: Member.Aux[ThrowableEither, U, Fx.fx1[IO]],
    io: _io[U],
    te: _throwableEither[U]
  ): Eff[U, A] = {
    // FIXME: Explore implementations that do not use unsafe Resource.allocated
    for {
      either <- fromIO[U, ThrowableEither[A]](
        dbResource.use(poolResource =>
          poolResource.use(session =>
            session
              .transaction(
                isolationLevel = isolationLevel,
                accessMode = TransactionAccessMode.ReadWrite
              )
              .use(_ =>
                IO.fromFuture(
                  IO {
                    runInternal[R, U, A](effects)(session).runEither[Throwable].unsafeToFuture
                  }
                )
              )
          )
        )
      )

      a <- fromEither[U, Throwable, A](either)
    } yield a
  }

  private def runInternal[R, U, A](effects: Eff[R, A])(session: Session[IO])(implicit
    m: Member.Aux[DBTransactionIO, R, U],
    io: _io[U],
    te: _throwableEither[U]
  ): Eff[U, A] = translate(effects)(new Translate[DBTransactionIO, U] {
    override def apply[X](sessionIO: DBTransactionIO[X]): Eff[U, X] =
      sessionIO match {
        case WithDBSession(f) =>
          fromIO[U, ThrowableEither[X]](
            f.execute[SkunkDBSession](SkunkDBSession(session)).attempt
          ).flatMap(either =>
            fromEither[U, Throwable, X](
              either.left.map(e => DBTransactionIOError.DatabaseError(e))
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
