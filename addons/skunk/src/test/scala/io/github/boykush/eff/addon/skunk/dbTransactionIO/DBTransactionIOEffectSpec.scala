package io.github.boykush.eff.addon.skunk.dbTransactionIO

import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.AbstractFreeSkunkDBSpec.Pet
import io.github.boykush.eff.addon.skunk.{AbstractFreeSkunkDBSpec, SkunkDBConfig, TestDB}
import io.github.boykush.eff.addon.skunk.dbTransactionIO.interpreter.SkunkDBTransactionIOInterpreter
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIOError
import io.github.boykush.eff.dbio.dbTransactionIO.DBTransactionIOTypes.DBTransactionIOStack
import io.github.boykush.eff.syntax.addon.skunk.dbTransactionIO.ToSkunkDBTransactionIOOps
import org.atnos.eff._
import org.atnos.eff.syntax.addon.cats.effect._
import org.atnos.eff.syntax.all._

class DBTransactionIOEffectSpec extends AbstractFreeSkunkDBSpec {

  trait SetUp {
    val testDBConfig: SkunkDBConfig = TestDB.skunkDBConfig

    type R = DBTransactionIOStack
    implicit val interpreter: SkunkDBTransactionIOInterpreter =
      new SkunkDBTransactionIOInterpreter(
        testDBConfig
      )

    case class ResourceNotFoundError(resourceName: String) extends DBTransactionIOError

    def checkDuplicateKeyError(result: Either[Throwable, Unit]): Boolean =
      result match {
        case Left(DBTransactionIOError.DatabaseError(e)) => {
          e.getMessage.contains("Duplicate key")
        }
        case Left(e)                                     => false
        case Right(_)                                    => false
      }
  }

  "#run" - {
    "Insert" in new SetUp {
      val pet: Pet = Pet.randomGen

      val effects: Eff[R, Unit] =
        DBTransactionIOEffect.store[R](session =>
          session.prepare(insertPet()).flatMap(pc => pc.execute(pet)).void
        )

      val result1: Either[Throwable, Unit] = await(
        effects.runDBTransactionIO
          .runEither[Throwable]
          .unsafeToFuture
      )

      result1 mustBe Right(())

      val result2: Option[Pet] = await(
        sessionUse(session =>
          session.prepare(selectPetByName).flatMap(_.option(pet.name))
        ).unsafeToFuture
      )

      result2 mustBe Some(pet)
    }

    "ResolveWithLock and Update" in new SetUp {
      val pet: Pet = Pet.randomGen

      await(
        sessionUse(session =>
          session.prepare(insertPet()).flatMap(_.execute(pet)).void
        ).unsafeToFuture
      )

      val updatedAge: Short = (pet.age + 1).toShort

      val effects: Eff[R, Unit] = for {
        exists <- DBTransactionIOEffect.resolveWithLock[R, Pet](session =>
          session.prepare(selectPetByNameWithLock).flatMap(_.option(pet.name))
        )
        updated = exists.copy(age = updatedAge)
        _ <- DBTransactionIOEffect.store[R](session =>
          session.prepare(updatePet()).flatMap(_.execute((updated.age, updated.name)).void)
        )
      } yield ()

      val result1: Either[Throwable, Unit] = await(
        effects.runDBTransactionIO.runEither[Throwable].unsafeToFuture
      )

      result1 mustBe Right(())

      val result2: Option[Pet] = await(
        sessionUse(session =>
          session.prepare(selectPetByName).flatMap(_.option(pet.name))
        ).unsafeToFuture
      )

      result2 mustBe Some(pet.copy(age = updatedAge))
    }
    "Catch DBTransactionIOError and rollback when occurred error" in new SetUp {
      val pet: Pet = Pet.randomGen

      val effects1: Eff[R, Unit] =
        DBTransactionIOEffect.store[R] { session =>
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
          _ <- DBTransactionIOEffect.store[R](
            _.prepare(insertPet()).flatMap(pc => pc.execute(pet)).void
          )
          _ <- DBTransactionIOEffect.store[R](
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
