package io.github.boykush.eff.addon.skunk.dbQueryIO.interpreter

import cats.effect._
import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.dbQueryIO.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.dbQueryIO.SkunkDBQueryIOEffect
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIOError
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIO
import io.github.boykush.eff.syntax.addon.skunk.dbQueryIO.ToSkunkDBQueryIOOps
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

    type R = Fx.fx3[DBQueryIO, IO, ThrowableEither]
    implicit val interpreter: SkunkDBQueryIOInterpreter = new SkunkDBQueryIOInterpreter(
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
      "Catch DBQueryIOError about cannot command in read-only session" in new SetUp {
        val uuid: String = UUID.randomUUID().toString

        val effects: Eff[R, Unit] =
          SkunkDBQueryIOEffect.withDBSession[R, Unit] { session =>
            for {
              _ <- session.execute(createTable()).void
            } yield ()
          }

        // Duplicate error occurred
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
