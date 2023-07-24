package io.github.boykush.eff.addon.skunk.dbTransactionIO.interpreter

import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.AbstractFreeSkunkDBSpec
import io.github.boykush.eff.addon.skunk.AbstractFreeSkunkDBSpec.Pet
import io.github.boykush.eff.addon.skunk.TestDB
import io.github.boykush.eff.addon.skunk.dbTransactionIO.SkunkDBTransactionIOEffect
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIOError
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIOTypes.DBTransactionIOStack
import io.github.boykush.eff.syntax.addon.skunk.dbTransactionIO.ToSkunkDBTransactionIOOps
import org.atnos.eff.Eff
import org.atnos.eff.syntax.addon.cats.effect._
import org.atnos.eff.syntax.all._

class SkunkDBTransactionIOInterpreterSpec extends AbstractFreeSkunkDBSpec {

  trait SetUp {
    val testDBConfig: SkunkDBConfig = TestDB.skunkDBConfig

    type R = DBTransactionIOStack
    implicit val interpreter: SkunkDBTransactionIOInterpreter =
      new SkunkDBTransactionIOInterpreter(
        testDBConfig
      )

    def checkDuplicateKeyError(result: Either[Throwable, Unit]): Boolean =
      result match {
        case Left(DBTransactionIOError(e)) => {
          e.getMessage.contains("Duplicate key")
        }
        case Left(e)                       => false
        case Right(_)                      => false
      }
  }

  "#run" - {
    "WithDBSession" - {
      "Insert and Select record" in new SetUp {
        val pet: Pet = Pet.randomGen

        val effects: Eff[R, Option[Pet]] =
          SkunkDBTransactionIOEffect.withDBSession[R, Option[Pet]] { session =>
            for {
              _        <- session.prepare(insertPet()).flatMap(pc => pc.execute(pet))
              maybePet <-
                session.prepare(selectPetByName).flatMap(pq => pq.option(pet.name))
            } yield maybePet
          }

        val result: Either[Throwable, Option[Pet]] = await(
          effects.runDBTransactionIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        result mustBe Right(Some(pet))
      }
      "Catch DBTransactionIOError and rollback when occurred error" in new SetUp {
        val pet: Pet = Pet.randomGen

        val effects1: Eff[R, Unit] =
          SkunkDBTransactionIOEffect.withDBSession[R, Unit] { session =>
            for {
              _ <- session.prepare(insertPet()).flatMap(pc => pc.execute(pet))
              _ <- session.prepare(insertPet()).flatMap(pc => pc.execute(pet))
            } yield ()
          }

        // Duplicate error occurred
        val result1: Either[Throwable, Unit] = await(
          effects1.runDBTransactionIO
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
            _ <- SkunkDBTransactionIOEffect.withDBSession[R, Unit](
              _.prepare(insertPet()).flatMap(pc => pc.execute(pet)).void
            )
            _ <- SkunkDBTransactionIOEffect.withDBSession[R, Unit](
              _.prepare(insertPet()).flatMap(pc => pc.execute(pet)).void
            )
          } yield ()
        }

        // Duplicate error occurred
        val result1: Either[Throwable, Unit] = await(
          effects1.runDBTransactionIO
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