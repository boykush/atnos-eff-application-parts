package io.github.boykush.eff.addon.skunk.dbSessionIO.interpreter

import cats.effect._
import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.dbSessionIO.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.dbSessionIO.SkunkDBSessionIOEffect
import io.github.boykush.eff.dbio.DBIOError
import io.github.boykush.eff.dbio.dbSessionIO.DBSessionIO
import io.github.boykush.eff.syntax.addon.skunk.dbSessionIO.ToSkunkDBSessionIOOps
import org.scalatest.freespec.AnyFreeSpec
import org.atnos.eff.Eff
import org.atnos.eff.Fx
import org.atnos.eff.syntax.addon.cats.effect._
import org.atnos.eff.all._
import org.atnos.eff.syntax.all._
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import skunk._
import skunk.codec.all._
import skunk.implicits._

import java.util.UUID
import scala.concurrent.Await

class SkunkDBSessionIOInterpreterSpec extends AnyFreeSpec with Matchers {

  trait SetUp {
    val testDBConfig: SkunkDBConfig = SkunkDBConfig(
      host = "localhost",
      port = 5432,
      user = "user",
      password = "password",
      database = "addons_skunk_test",
      maxConnections = 1
    )

    type R = Fx.fx3[DBSessionIO, IO, ThrowableEither]
    implicit val interpreter: SkunkDBSessionIOInterpreter = new SkunkDBSessionIOInterpreter(
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
  }

  "#run" - {
    "WithDBSession" - {
      "Insert and Select records via interpreter" in new SetUp {
        val uuid: String = UUID.randomUUID().toString

        val effects: Eff[R, Option[String]] =
          SkunkDBSessionIOEffect.withDBSession[R, Option[String]] { session =>
            for {
              _    <- session.execute(createTable()).void
              _    <- session.prepare(insert()).flatMap(pc => pc.execute(uuid))
              name <- session.prepare(select).flatMap(pq => pq.option(uuid))
            } yield name
          }

        val result: Either[Throwable, Option[String]] = Await.result(
          effects.runDBSessionIO
            .runEither[Throwable]
            .unsafeToFuture,
          1.minutes
        )

        result mustBe Right(Some(uuid))
      }

      "Catch DBIOError about duplicated key" in new SetUp {
        val uuid: String = UUID.randomUUID().toString

        val effects: Eff[R, Unit] =
          SkunkDBSessionIOEffect.withDBSession[R, Unit] { session =>
            for {
              _ <- session.execute(createTable()).void
              _ <- session.prepare(insert()).flatMap(pc => pc.execute(uuid))
              _ <- session.prepare(insert()).flatMap(pc => pc.execute(uuid))
            } yield ()
          }

        // Duplicate error occurred
        val result: Either[Throwable, Unit] = Await.result(
          effects.runDBSessionIO
            .runEither[Throwable]
            .unsafeToFuture,
          1.minutes
        )

        result match {
          case Left(DBIOError(message)) => {
            message.contains("Duplicate key value violates unique constraint") mustBe true
          }
          case _                        => fail
        }
      }
    }
  }
}
