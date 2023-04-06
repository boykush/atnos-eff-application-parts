package io.github.boykush.eff.addon.skunk.dbCommandIO.interpreter

import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.AbstractFreeSkunkDBSpec
import io.github.boykush.eff.addon.skunk.AbstractFreeSkunkDBSpec.Pet
import io.github.boykush.eff.addon.skunk.TestDB
import io.github.boykush.eff.addon.skunk.dbCommandIO.SkunkDBCommandIOEffect
import io.github.boykush.eff.dbio.dbCommandIO.DBCommandIOError
import io.github.boykush.eff.dbio.dbCommandIO.DBCommandIOTypes.DBCommandIOStack
import io.github.boykush.eff.syntax.addon.skunk.dbCommandIO.ToSkunkDBCommandIOOps
import org.atnos.eff.Eff
import org.atnos.eff.syntax.addon.cats.effect._
import org.atnos.eff.syntax.all._

class SkunkDBCommandIOInterpreterSpec extends AbstractFreeSkunkDBSpec {

  trait SetUp {
    val testDBConfig: SkunkDBConfig = TestDB.skunkDBConfig

    type R = DBCommandIOStack
    implicit val interpreter: SkunkDBCommandIOInterpreter =
      new SkunkDBCommandIOInterpreter(
        testDBConfig
      )

    def checkDuplicateKeyError(result: Either[Throwable, Unit]): Boolean =
      result match {
        case Left(DBCommandIOError(e)) => {
          e.getMessage.contains("Duplicate key")
        }
        case Left(e)                   => false
        case Right(_)                  => false
      }
  }

  "#run" - {
    "WithDBSession" - {
      "Insert and Select record" in new SetUp {
        val pet: Pet = Pet.randomGen

        val effects: Eff[R, Option[Pet]] =
          SkunkDBCommandIOEffect.withDBSession[R, Option[Pet]] { session =>
            for {
              _        <- session.prepare(insertPet()).flatMap(pc => pc.execute(pet))
              maybePet <-
                session.prepare(selectPetByName).flatMap(pq => pq.option(pet.name))
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
              _ <- session.prepare(insertPet()).flatMap(pc => pc.execute(pet))
              _ <- session.prepare(insertPet()).flatMap(pc => pc.execute(pet))
            } yield ()
          }

        // Duplicate error occurred
        val result1: Either[Throwable, Unit] = await(
          effects1.runDBCommandIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        checkDuplicateKeyError(result1) mustBe true

        val result2: Option[Pet] = await(
          sessionUse(session => session.prepare(selectPetByName).flatMap(pq => pq.option(pet.name)))
            .unsafeToFuture()
        )

        result2 mustBe None
      }
      "Sessions are shared within a single interpreter run" in new SetUp {
        val pet: Pet = Pet.randomGen

        val effects1: Eff[R, Unit] = {
          for {
            _ <- SkunkDBCommandIOEffect.withDBSession[R, Unit](
              _.prepare(insertPet()).flatMap(pc => pc.execute(pet)).void
            )
            _ <- SkunkDBCommandIOEffect.withDBSession[R, Unit](
              _.prepare(insertPet()).flatMap(pc => pc.execute(pet)).void
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

        val result2: Option[Pet] = await(
          sessionUse(session => session.prepare(selectPetByName).flatMap(pq => pq.option(pet.name)))
            .unsafeToFuture()
        )

        // All commands without an eff context are rolled back
        result2 mustBe None
      }
    }
  }
}
