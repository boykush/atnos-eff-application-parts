package io.github.boykush.eff.addon.skunk.dbReadIO.interpreter

import com.google.inject.Inject
import cats.effect._
import cats.effect.kernel.Resource
import cats.effect.unsafe.IORuntime
import io.github.boykush.eff.addon.skunk.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.SkunkDBSession
import io.github.boykush.eff.dbio.dbReadIO.DBReadIO
import io.github.boykush.eff.dbio.dbReadIO.DBReadIOError
import io.github.boykush.eff.dbio.dbReadIO.Pager
import io.github.boykush.eff.dbio.dbReadIO.WithDBSession
import io.github.boykush.eff.dbio.dbReadIO.WithDBSessionAndPager
import io.github.boykush.eff.dbio.dbReadIO.interpreter.DBReadIOInterpreter
import natchez.Trace.Implicits.noop
import org.atnos.eff.Interpret._
import org.atnos.eff._
import org.atnos.eff.addon.cats.effect.IOEffect._io
import org.atnos.eff.addon.cats.effect.IOEffect.fromIO
import org.atnos.eff.all._
import org.atnos.eff.syntax.addon.cats.effect.toIOOps
import org.atnos.eff.syntax.all.toEitherEffectOps
import skunk._
import skunk.data.TransactionAccessMode
import skunk.data.TransactionIsolationLevel

class SkunkDBReadIOInterpreter @Inject() (
  config: SkunkDBConfig,
  isolationLevel: TransactionIsolationLevel = TransactionIsolationLevel.RepeatableRead
) extends DBReadIOInterpreter {

  implicit val runtime: IORuntime = IORuntime.global

  def run[R, U, A](effects: Eff[R, A], pager: Pager)(implicit
    m1: Member.Aux[DBReadIO, R, U],
    m2: Member.Aux[ThrowableEither, U, Fx.fx1[IO]],
    io: _io[U],
    te: _throwableEither[U]
  ): Eff[U, A] = for {
    either <- fromIO[U, ThrowableEither[A]](dbResource.use { pooled =>
      pooled.use { session =>
        session
          .transaction(
            isolationLevel = isolationLevel,
            accessMode = TransactionAccessMode.ReadOnly
          )
          .use { _ =>
            IO.fromFuture(
              IO {
                runInternal[R, U, A](effects, pager)(session).runEither[Throwable].unsafeToFuture
              }
            )
          }
      }
    })

    a <- fromEither[U, Throwable, A](either)
  } yield a

  private def runInternal[R, U, A](effects: Eff[R, A], pager: Pager)(session: Session[IO])(implicit
    m: Member.Aux[DBReadIO, R, U],
    io: _io[U],
    te: _throwableEither[U]
  ): Eff[U, A] = translate(effects)(new Translate[DBReadIO, U] {
    override def apply[X](readIO: DBReadIO[X]): Eff[U, X] = {
      fromIO[U, ThrowableEither[X]](readIO match {
        case WithDBSession(sessionF)                 =>
          sessionF.execute[SkunkDBSession](SkunkDBSession(session)).attempt
        case WithDBSessionAndPager(sessionAndPagerF) =>
          sessionAndPagerF.execute[SkunkDBSession](SkunkDBSession(session), pager).attempt
      }).flatMap(either =>
        fromEither[U, Throwable, X](either.left.map(e => DBReadIOError.DatabaseError(e)))
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
