package io.github.boykush.eff.addon.skunk.dbReadIO.interpreter

import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.AbstractFreeSkunkDBSpec
import io.github.boykush.eff.addon.skunk.AbstractFreeSkunkDBSpec.Pet
import io.github.boykush.eff.addon.skunk.TestDB
import io.github.boykush.eff.addon.skunk.dbReadIO.SkunkDBReadIOEffect
import io.github.boykush.eff.dbio.dbReadIO.DBReadIOError
import io.github.boykush.eff.dbio.dbReadIO.DBReadIOEffect._
import io.github.boykush.eff.syntax.addon.skunk.dbReadIO.ToSkunkDBReadIOOps
import org.atnos.eff.Eff
import org.atnos.eff.syntax.addon.cats.effect._
import org.atnos.eff.syntax.all._

class SkunkDBReadIOInterpreterSpec extends AbstractFreeSkunkDBSpec {

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
          SkunkDBReadIOEffect.withDBSession[R, Option[Pet]] { session =>
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
          SkunkDBReadIOEffect.withDBSession[R, Unit] { session =>
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
          case Left(DBReadIOError(e)) => {
            e.getMessage.contains("read-only") mustBe true
          }
          case _                      => fail
        }
      }
    }
  }
}
