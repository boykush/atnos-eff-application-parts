package io.github.boykush.eff.addon.skunk.dbQueryIO.interpreter

import cats.effect._
import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.dbCommandIO.SkunkDBCommandIOEffect
import io.github.boykush.eff.addon.skunk.dbCommandIO.interpreter.SkunkDBCommandIOInterpreter
import io.github.boykush.eff.addon.skunk.dbQueryIO.SkunkDBQueryIOEffect
import io.github.boykush.eff.dbio.dbCommandIO.DBCommandIO
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIOError
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIO
import io.github.boykush.eff.syntax.addon.skunk.dbQueryIO.ToSkunkDBQueryIOOps
import io.github.boykush.eff.syntax.addon.skunk.dbCommandIO.ToSkunkDBCommandIOOps
import org.scalatest.freespec.AnyFreeSpec
import org.atnos.eff.Eff
import org.atnos.eff.Fx
import org.atnos.eff.syntax.addon.cats.effect._
import org.atnos.eff.all._
import org.atnos.eff.either.errorTranslate
import org.atnos.eff.syntax.all._
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import skunk._
import skunk.codec.all._
import skunk.implicits._

import java.util.UUID
import scala.concurrent.Await

class SkunkDBQueryIOInterpreterSpec extends AnyFreeSpec with Matchers {

  trait SetUp {
    val testDBConfig: SkunkDBConfig = SkunkDBConfig(
      host = "localhost",
      port = 5432,
      user = "user",
      password = "password",
      database = "addons_skunk_test",
      maxConnections = 1
    )

    type DBCommandIOStack = Fx.fx3[DBCommandIO, IO, ThrowableEither]
    type DBQueryIOStack   = Fx.fx3[DBQueryIO, IO, ThrowableEither]
    implicit val dbQueryIOInterpreter: SkunkDBQueryIOInterpreter = new SkunkDBQueryIOInterpreter(
      testDBConfig
    )
    implicit val dbCommandIOInterpreter: SkunkDBCommandIOInterpreter =
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
  }

  "#run" - {
    "WithDBSession" - {
      "Select record" in new SetUp {
        val uuid: String = UUID.randomUUID().toString

        val effects1: Eff[DBCommandIOStack, Unit] =
          SkunkDBCommandIOEffect.withDBSession[DBCommandIOStack, Unit] { session =>
            for {
              _ <- session.execute(createTable()).void
              _ <- session.prepare(insert()).flatMap(pc => pc.execute(uuid))
            } yield ()
          }

        val result1: Either[Throwable, Unit] = Await.result(
          effects1.runDBCommandIO
            .runEither[Throwable]
            .unsafeToFuture,
          1.minutes
        )

        result1.isRight mustBe true

        val effects2: Eff[DBQueryIOStack, Option[String]] =
          SkunkDBQueryIOEffect.withDBSession[DBQueryIOStack, Option[String]] { session =>
            session.prepare(select).flatMap(pq => pq.option(uuid))
          }

        val result2: Either[Throwable, Option[String]] = Await.result(
          effects2.runDBQueryIO
            .runEither[Throwable]
            .unsafeToFuture,
          1.minutes
        )

        result2 mustBe Right(Some(uuid))
      }
      "Catch DBQueryIOError about cannot command in read-only session" in new SetUp {
        val effects: Eff[DBQueryIOStack, Unit] =
          SkunkDBQueryIOEffect.withDBSession[DBQueryIOStack, Unit] { session =>
            for {
              _ <- session.execute(createTable()).void
            } yield ()
          }

        val result: Either[Throwable, Unit] = Await.result(
          effects.runDBQueryIO
            .runEither[Throwable]
            .unsafeToFuture,
          1.minutes
        )

        result match {
          case Left(DBQueryIOError(message)) => {
            message.contains("read-only") mustBe true
          }
          case _                             => fail
        }
      }
    }
  }
}
