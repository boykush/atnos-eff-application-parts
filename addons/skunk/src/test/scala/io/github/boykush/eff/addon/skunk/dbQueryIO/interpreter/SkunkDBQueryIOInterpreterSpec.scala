package io.github.boykush.eff.addon.skunk.dbQueryIO.interpreter

import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.AbstractFreeSpec
import io.github.boykush.eff.addon.skunk.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.TestDB
import io.github.boykush.eff.addon.skunk.dbCommandIO.SkunkDBCommandIOEffect
import io.github.boykush.eff.addon.skunk.dbCommandIO.interpreter.SkunkDBCommandIOInterpreter
import io.github.boykush.eff.addon.skunk.dbQueryIO.SkunkDBQueryIOEffect
import io.github.boykush.eff.dbio.dbCommandIO.DBCommandIOEffect._
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIOError
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIOEffect._
import io.github.boykush.eff.syntax.addon.skunk.dbQueryIO.ToSkunkDBQueryIOOps
import io.github.boykush.eff.syntax.addon.skunk.dbCommandIO.ToSkunkDBCommandIOOps
import org.atnos.eff.Eff
import org.atnos.eff.syntax.addon.cats.effect._
import org.atnos.eff.syntax.all._
import skunk._
import skunk.codec.all._
import skunk.implicits._

import java.util.UUID

class SkunkDBQueryIOInterpreterSpec extends AbstractFreeSpec {

  trait SetUp {
    val testDBConfig: SkunkDBConfig = TestDB.skunkDBConfig

    type R1 = DBCommandIOStack
    type R2 = DBQueryIOStack
    implicit val dbQueryIOInterpreter: SkunkDBQueryIOInterpreter = new SkunkDBQueryIOInterpreter(
      testDBConfig
    )
    implicit val dbCommandIOInterpreter: SkunkDBCommandIOInterpreter =
      new SkunkDBCommandIOInterpreter(
        testDBConfig
      )

    def createTable(): Command[Void] =
      sql"CREATE TABLE IF NOT EXISTS skunk_db_query_io (name varchar unique)".command

    def insert(): Command[String] =
      sql"""
              INSERT INTO skunk_db_query_io VALUES ($varchar);
         """.command

    def select: Query[String, String] =
      sql"""
              SELECT name FROM skunk_db_query_io WHERE name = $varchar;
         """.query(varchar)
  }

  "#run" - {
    "WithDBSession" - {
      "Select record" in new SetUp {
        val uuid: String = UUID.randomUUID().toString

        val effects1: Eff[R1, Unit] =
          SkunkDBCommandIOEffect.withDBSession[R1, Unit] { session =>
            for {
              _ <- session.execute(createTable()).void
              _ <- session.prepare(insert()).flatMap(pc => pc.execute(uuid))
            } yield ()
          }

        val result1: Either[Throwable, Unit] = await(
          effects1.runDBCommandIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        result1.isRight mustBe true

        val effects2: Eff[R2, Option[String]] =
          SkunkDBQueryIOEffect.withDBSession[R2, Option[String]] { session =>
            session.prepare(select).flatMap(pq => pq.option(uuid))
          }

        val result2: Either[Throwable, Option[String]] = await(
          effects2.runDBQueryIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        result2 mustBe Right(Some(uuid))
      }
      "Catch DBQueryIOError about cannot command in read-only session" in new SetUp {
        val effects: Eff[R2, Unit] =
          SkunkDBQueryIOEffect.withDBSession[R2, Unit] { session =>
            for {
              _ <- session.execute(createTable()).void
            } yield ()
          }

        val result: Either[Throwable, Unit] = await(
          effects.runDBQueryIO
            .runEither[Throwable]
            .unsafeToFuture
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
