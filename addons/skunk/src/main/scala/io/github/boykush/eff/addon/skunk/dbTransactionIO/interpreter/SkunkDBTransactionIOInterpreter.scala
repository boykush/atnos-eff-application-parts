package io.github.boykush.eff.addon.skunk.dbTransactionIO.interpreter

import cats.effect._
import cats.effect.kernel.Resource
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
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.addon.cats.effect.IOEffect.fromIO
import org.atnos.eff.all._
import skunk._
import skunk.data.TransactionAccessMode
import skunk.data.TransactionIsolationLevel

class SkunkDBTransactionIOInterpreter @Inject() (
  config: SkunkDBConfig,
  isolationLevel: TransactionIsolationLevel = TransactionIsolationLevel.RepeatableRead
) extends DBTransactionIOInterpreter {
  def run[R, U, A](effects: Eff[R, A])(implicit
    m: Member.Aux[DBTransactionIO, R, U],
    io: _io[U],
    te: _throwableEither[U]
  ): Eff[U, A] =
    for {
      /** allocate(open) resource */
      poolAllocated <- fromIO(dbResource.allocated)
      (poolResource, poolReleaser) = poolAllocated
      sessionAllocated <- fromIO(poolResource.allocated)
      (session, sessionCloser) = sessionAllocated
      transactionAllocated <- fromIO(
        session
          .transaction(
            isolationLevel = isolationLevel,
            accessMode = TransactionAccessMode.ReadWrite
          )
          .allocated
      )
      (_, transactionCloser) = transactionAllocated
      /** use resource */
      result <- runInternal(effects)(session)
      /** close resource */
      _      <- fromIO(transactionCloser)
      _      <- fromIO(sessionCloser)
      _      <- fromIO(poolReleaser)
    } yield result

  private def runInternal[R, U, A](effects: Eff[R, A])(session: Session[IO])(implicit
    m: Member.Aux[DBTransactionIO, R, U],
    io: _io[U],
    te: _throwableEither[U]
  ): Eff[U, A] = translate(effects)(new Translate[DBTransactionIO, U] {
    override def apply[X](sessionIO: DBTransactionIO[X]): Eff[U, X] =
      sessionIO match {
        case WithDBSession(f: (SkunkDBSession => IO[X])) =>
          fromIO[U, ThrowableEither[X]](
            f(SkunkDBSession(session)).attempt
          ).flatMap(either =>
            fromEither[U, Throwable, X](
              either.left.map(e => DBTransactionIOError(e))
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
