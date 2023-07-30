package io.github.boykush.eff.addon.skunk.dbReadIO

import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.AbstractFreeSkunkDBSpec.Pet
import io.github.boykush.eff.addon.skunk.AbstractFreeSkunkDBSpec
import io.github.boykush.eff.addon.skunk.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.TestDB
import io.github.boykush.eff.addon.skunk.dbReadIO.interpreter.SkunkDBReadIOInterpreter
import io.github.boykush.eff.dbio.dbReadIO.DBReadIOEffect._
import io.github.boykush.eff.dbio.dbReadIO.DBReadIOError
import io.github.boykush.eff.dbio.dbReadIO.Pager
import io.github.boykush.eff.syntax.addon.skunk.dbReadIO.ToSkunkDBReadIOOps
import org.atnos.eff.Eff
import org.atnos.eff.syntax.addon.cats.effect._
import org.atnos.eff.syntax.all._

class DBReadIOEffectSpec extends AbstractFreeSkunkDBSpec {

  trait SetUp {
    val testDBConfig: SkunkDBConfig = TestDB.skunkDBConfig
    type R = DBReadIOStack
    implicit val dbReadIOInterpreter: SkunkDBReadIOInterpreter = new SkunkDBReadIOInterpreter(
      testDBConfig
    )
  }

  "#run" - {
    "WithDBSession" - {
      "Select record" in new SetUp {
        val pet: Pet = Pet.randomGen

        await(
          sessionUse(session => session.prepare(insertPet()).flatMap(pc => pc.execute(pet)))
            .unsafeToFuture()
        )

        val effects: Eff[R, Option[Pet]] =
          DBReadIOEffect.withDBSession[R, Option[Pet]] { session =>
            session.prepare(selectPetByName).flatMap(pq => pq.option(pet.name))
          }

        val result: Either[Throwable, Option[Pet]] = await(
          effects.runDBReadIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        result mustBe Right(Some(pet))
      }
      "Catch DBReadIOError about cannot command in read-only session" in new SetUp {
        val pet: Pet = Pet.randomGen

        val effects: Eff[R, Unit] =
          DBReadIOEffect.withDBSession[R, Unit] { session =>
            for {
              _ <- session.prepare(insertPet()).flatMap(pc => pc.execute(pet))
            } yield ()
          }

        val result: Either[Throwable, Unit] = await(
          effects.runDBReadIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        result match {
          case Left(DBReadIOError.DatabaseError(e)) => {
            e.getMessage.contains("read-only") mustBe true
          }
          case _                                    => fail
        }
      }
    }
    "WithDBSessionAndPager" - {
      "List records without paging" in new SetUp {
        val pet1: Pet = Pet.randomGen
        val pet2: Pet = Pet.randomGen

        await(
          sessionUse(session =>
            for {
              _ <- session.prepare(insertPet()).flatMap(pc => pc.execute(pet1))
              _ <- session.prepare(insertPet()).flatMap(pc => pc.execute(pet2))
            } yield ()
          )
            .unsafeToFuture()
        )

        val effects: Eff[R, List[Pet]] =
          DBReadIOEffect.withDBSessionAndPager[R, Pet] { (session, pager) =>
            session
              .prepare(selectPet)
              .flatMap(pq => pq.stream((pager.limit, pager.offset), 100).compile.toList)
          }

        val result: Either[Throwable, List[Pet]] = await(
          effects.runDBReadIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        result mustBe Right(List(pet1, pet2))
      }

      "List records by paging" in new SetUp {
        val pet1: Pet = Pet.randomGen
        val pet2: Pet = Pet.randomGen

        await(
          sessionUse(session =>
            for {
              _ <- session.prepare(insertPet()).flatMap(pc => pc.execute(pet1))
              _ <- session.prepare(insertPet()).flatMap(pc => pc.execute(pet2))
            } yield ()
          )
            .unsafeToFuture()
        )

        val effects1: Eff[R, List[Pet]] =
          DBReadIOEffect.withDBSessionAndPager[R, Pet] { (session, pager) =>
            session
              .prepare(selectPet)
              .flatMap(pq => pq.stream((pager.limit, pager.offset), 100).compile.toList)
          }

        val result1: Either[Throwable, List[Pet]] = await(
          effects1
            .runDBReadIOWithPager(Pager(1, 0))
            .runEither[Throwable]
            .unsafeToFuture
        )

        result1 mustBe Right(List(pet1))

        val effects2: Eff[R, List[Pet]] =
          DBReadIOEffect.withDBSessionAndPager[R, Pet] { (session, pager) =>
            session
              .prepare(selectPet)
              .flatMap(pq => pq.stream((pager.limit, pager.offset), 100).compile.toList)
          }

        val result2: Either[Throwable, List[Pet]] = await(
          effects2
            .runDBReadIOWithPager(Pager(1, 1))
            .runEither[Throwable]
            .unsafeToFuture
        )

        result2 mustBe Right(List(pet2))
      }
    }
  }
}
