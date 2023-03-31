package io.github.boykush.eff.addon.skunk.dbCommandIO.interpreter

import cats.effect._
import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.dbCommandIO.SkunkDBCommandIOEffect
import io.github.boykush.eff.dbio.dbCommandIO.DBCommandIO
import io.github.boykush.eff.dbio.dbCommandIO.DBCommandIOError
import io.github.boykush.eff.syntax.addon.skunk.dbCommandIO.ToSkunkDBCommandIOOps
import org.atnos.eff.Eff
import org.atnos.eff.Fx
import org.atnos.eff.all._
import org.atnos.eff.either.errorTranslate
import org.atnos.eff.syntax.addon.cats.effect._
import org.atnos.eff.syntax.all._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import skunk._
import skunk.codec.all._
import skunk.implicits._

import java.util.UUID
import scala.concurrent.Await

class SkunkDBCommandIOInterpreterSpec extends AnyFreeSpec with Matchers {

  trait SetUp {
    val testDBConfig: SkunkDBConfig = SkunkDBConfig(
      host = "localhost",
      port = 5432,
      user = "user",
      password = "password",
      database = "addons_skunk_test",
      maxConnections = 1
    )

    type R = Fx.fx3[DBCommandIO, IO, ThrowableEither]
    implicit val interpreter: SkunkDBCommandIOInterpreter =
      new SkunkDBCommandIOInterpreter(
        testDBConfig
      )

    def createTable(): Command[Void] =
      sql"CREATE TABLE IF NOT EXISTS skunk_session_io (name varchar unique)".command

    def insert(): Command[String] =
      sql"""
              INSERT INTO skunk_session_io VALUES ($varchar);
         """.command

    def select: Query[String, String] =
      sql"""
              SELECT name FROM skunk_session_io WHERE name = $varchar;
         """.query(varchar)

    def checkDuplicateKeyError(result: Either[Throwable, Unit]): Boolean =
      result match {
        case Left(DBCommandIOError(message)) => {
          message.contains("Duplicate key")
        }
        case Left(e)                         => false
        case Right(_)                        => false
      }
  }

  "#run" - {
    "WithDBSession" - {
      "Insert and Select record" in new SetUp {
        val uuid: String = UUID.randomUUID().toString

        val effects: Eff[R, Option[String]] =
          SkunkDBCommandIOEffect.withDBSession[R, Option[String]] { session =>
            for {
              _    <- session.execute(createTable()).void
              _    <- session.prepare(insert()).flatMap(pc => pc.execute(uuid))
              name <- session.prepare(select).flatMap(pq => pq.option(uuid))
            } yield name
          }

        val result: Either[Throwable, Option[String]] = Await.result(
          effects.runDBCommandIO
            .runEither[Throwable]
            .unsafeToFuture,
          1.minutes
        )

        result mustBe Right(Some(uuid))
      }
      "Catch DBCommandIOError and rollback when occurred error" in new SetUp {
        val uuid: String = UUID.randomUUID().toString

        val effects1: Eff[R, Unit] =
          SkunkDBCommandIOEffect.withDBSession[R, Unit] { session =>
            for {
              _ <- session.execute(createTable()).void
              _ <- session.prepare(insert()).flatMap(pc => pc.execute(uuid))
              _ <- session.prepare(insert()).flatMap(pc => pc.execute(uuid))
            } yield ()
          }

        // Duplicate error occurred
        val result1: Either[Throwable, Unit] = Await.result(
          effects1.runDBCommandIO
            .runEither[Throwable]
            .unsafeToFuture,
          1.minutes
        )

        checkDuplicateKeyError(result1) mustBe true

        val effects2: Eff[R, Option[String]] =
          SkunkDBCommandIOEffect.withDBSession[R, Option[String]] { session =>
            session.prepare(select).flatMap(pq => pq.option(uuid))
          }

        val result2: Either[Throwable, Option[String]] = Await.result(
          effects2.runDBCommandIO
            .runEither[Throwable]
            .unsafeToFuture,
          1.minutes
        )

        result2 mustBe Right(None)
      }
      "Sessions are shared within a single interpreter run" in new SetUp {
        val uuid: String = UUID.randomUUID().toString

        val effects1: Eff[R, Unit] = {
          for {
            _ <- SkunkDBCommandIOEffect.withDBSession[R, Unit](_.execute(createTable()).void)
            _ <- SkunkDBCommandIOEffect.withDBSession[R, Unit](
              _.prepare(insert()).flatMap(pc => pc.execute(uuid)).void
            )
            _ <- SkunkDBCommandIOEffect.withDBSession[R, Unit](
              _.prepare(insert()).flatMap(pc => pc.execute(uuid)).void
            )
          } yield ()
        }

        // Duplicate error occurred
        val result1: Either[Throwable, Unit] = Await.result(
          effects1.runDBCommandIO
            .runEither[Throwable]
            .unsafeToFuture,
          1.minutes
        )

        checkDuplicateKeyError(result1) mustBe true

        val effects2: Eff[R, Option[String]] =
          SkunkDBCommandIOEffect.withDBSession[R, Option[String]](session =>
            session.prepare(select).flatMap(pq => pq.option(uuid))
          )

        val result2: Either[Throwable, Option[String]] = Await.result(
          effects2.runDBCommandIO
            .runEither[Throwable]
            .unsafeToFuture,
          1.minutes
        )

        // All commands without an eff context are rolled back
        result2 mustBe Right(None)
      }
    }
  }
}
