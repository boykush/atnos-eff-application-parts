package io.github.boykush.eff.addon.skunk.dbCommandIO.interpreter

import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.AbstractFreeSpec
import io.github.boykush.eff.addon.skunk.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.SkunkTestPetService
import io.github.boykush.eff.addon.skunk.SkunkTestPetService.Pet
import io.github.boykush.eff.addon.skunk.TestDB
import io.github.boykush.eff.addon.skunk.dbCommandIO.SkunkDBCommandIOEffect
import io.github.boykush.eff.dbio.dbCommandIO.DBCommandIOError
import io.github.boykush.eff.dbio.dbCommandIO.DBCommandIOTypes.DBCommandIOStack
import io.github.boykush.eff.syntax.addon.skunk.dbCommandIO.ToSkunkDBCommandIOOps
import org.atnos.eff.Eff
import org.atnos.eff.syntax.addon.cats.effect._
import org.atnos.eff.syntax.all._


class SkunkDBCommandIOInterpreterSpec extends AbstractFreeSpec {

  trait SetUp {
    val testDBConfig: SkunkDBConfig = TestDB.skunkDBConfig

    type R = DBCommandIOStack
    implicit val interpreter: SkunkDBCommandIOInterpreter =
      new SkunkDBCommandIOInterpreter(
        testDBConfig
      )

    val petService: SkunkTestPetService = new SkunkTestPetService("skunk_db_command_io")

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
        val pet: Pet = Pet.randomGen

        val effects: Eff[R, Option[Pet]] =
          SkunkDBCommandIOEffect.withDBSession[R, Option[Pet]] { session =>
            for {
              _        <- session.execute(petService.createTable).void
              _        <- session.prepare(petService.insert).flatMap(pc => pc.execute(pet))
              maybePet <-
                session.prepare(petService.selectByName).flatMap(pq => pq.option(pet.name))
            } yield maybePet
          }

        val result: Either[Throwable, Option[Pet]] = await(
          effects.runDBCommandIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        result mustBe Right(Some(pet))
      }
      "Catch DBCommandIOError and rollback when occurred error" in new SetUp {
        val pet: Pet = Pet.randomGen

        val effects1: Eff[R, Unit] =
          SkunkDBCommandIOEffect.withDBSession[R, Unit] { session =>
            for {
              _ <- session.execute(petService.createTable).void
              _ <- session.prepare(petService.insert).flatMap(pc => pc.execute(pet))
              _ <- session.prepare(petService.insert).flatMap(pc => pc.execute(pet))
            } yield ()
          }

        // Duplicate error occurred
        val result1: Either[Throwable, Unit] = await(
          effects1.runDBCommandIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        checkDuplicateKeyError(result1) mustBe true

        val effects2: Eff[R, Option[Pet]] =
          SkunkDBCommandIOEffect.withDBSession[R, Option[Pet]] { session =>
            session.prepare(petService.selectByName).flatMap(pq => pq.option(pet.name))
          }

        val result2: Either[Throwable, Option[Pet]] = await(
          effects2.runDBCommandIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        result2 mustBe Right(None)
      }
      "Sessions are shared within a single interpreter run" in new SetUp {
        val pet: Pet = Pet.randomGen

        val effects1: Eff[R, Unit] = {
          for {
            _ <-
              SkunkDBCommandIOEffect.withDBSession[R, Unit](_.execute(petService.createTable).void)
            _ <- SkunkDBCommandIOEffect.withDBSession[R, Unit](
              _.prepare(petService.insert).flatMap(pc => pc.execute(pet)).void
            )
            _ <- SkunkDBCommandIOEffect.withDBSession[R, Unit](
              _.prepare(petService.insert).flatMap(pc => pc.execute(pet)).void
            )
          } yield ()
        }

        // Duplicate error occurred
        val result1: Either[Throwable, Unit] = await(
          effects1.runDBCommandIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        checkDuplicateKeyError(result1) mustBe true

        val effects2: Eff[R, Option[Pet]] =
          SkunkDBCommandIOEffect.withDBSession[R, Option[Pet]](session =>
            session.prepare(petService.selectByName).flatMap(pq => pq.option(pet.name))
          )

        val result2: Either[Throwable, Option[Pet]] = await(
          effects2.runDBCommandIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        // All commands without an eff context are rolled back
        result2 mustBe Right(None)
      }
    }
  }
}
