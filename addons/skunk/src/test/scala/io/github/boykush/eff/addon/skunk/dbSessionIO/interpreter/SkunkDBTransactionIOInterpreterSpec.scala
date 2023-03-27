package io.github.boykush.eff.addon.skunk.dbSessionIO.interpreter

import cats.effect._
import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.dbSessionIO.{SkunkDBConfig, SkunkDBSessionIOEffect}
import io.github.boykush.eff.dbio.dbSessionIO.DBSessionIO
import io.github.boykush.eff.syntax.addon.skunk.dbSessionIO.ToSkunkDBSessionIOOps
import org.scalatest.freespec.AnyFreeSpec
import org.atnos.eff.{Eff, Fx}
import org.atnos.eff.syntax.addon.cats.effect._
import org.scalatest.matchers.must.Matchers
import skunk._
import skunk.codec.all._
import skunk.implicits._

import java.util.UUID

class SkunkDBTransactionIOInterpreterSpec extends AnyFreeSpec with Matchers {

  trait SetUp {
    val testDBConfig: SkunkDBConfig = SkunkDBConfig(
      host = "localhost",
      port = 5432,
      user = "user",
      password = "password",
      database = "addons_skunk_test",
      maxConnections = 1
    )

    type R = Fx.fx2[DBSessionIO, IO]
    implicit val interpreter: SkunkDBSessionIOInterpreter = new SkunkDBSessionIOInterpreter(
      testDBConfig
    )
  }

  "#run" - {
    "Insert and Select records via interpreter" in new SetUp {
      val uuid: String = UUID.randomUUID().toString

      def createTable(): Command[Void]  =
        sql"CREATE TEMP TABLE pets (name varchar unique)".command
      def insertPets(): Command[String] =
        sql"""
              INSERT INTO pets VALUES ($varchar);
           """.command

      def selectPets: Query[String, String] =
        sql"""
              SELECT name FROM pets WHERE name = $varchar;
           """.query(varchar)

      val effects: Eff[R, Option[String]] =
        SkunkDBSessionIOEffect.withDBSession[R, Option[String]] { session =>
          for {
            _    <- session.execute(createTable()).void
            _    <- session.prepare(insertPets()).flatMap(pc => pc.execute(uuid))
            name <- session.prepare(selectPets).flatMap(pq => pq.option(uuid))
          } yield name
        }

      effects.runDBSessionIO
        .unsafeRunAsync {
          case Left(e)  => fail(e)
          case Right(r) => r mustBe Some(uuid)
        }
    }
  }
}
