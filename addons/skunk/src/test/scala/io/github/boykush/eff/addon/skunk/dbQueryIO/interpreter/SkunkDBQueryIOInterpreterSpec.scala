package io.github.boykush.eff.addon.skunk.dbQueryIO.interpreter

import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.SkunkDBConfig
import io.github.boykush.eff.addon.skunk.AbstractFreeSkunkDBSpec
import io.github.boykush.eff.addon.skunk.AbstractFreeSkunkDBSpec.Pet
import io.github.boykush.eff.addon.skunk.TestDB
import io.github.boykush.eff.addon.skunk.dbQueryIO.SkunkDBQueryIOEffect
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIOError
import io.github.boykush.eff.dbio.dbQueryIO.DBQueryIOEffect._
import io.github.boykush.eff.syntax.addon.skunk.dbQueryIO.ToSkunkDBQueryIOOps
import org.atnos.eff.Eff
import org.atnos.eff.syntax.addon.cats.effect._
import org.atnos.eff.syntax.all._

class SkunkDBQueryIOInterpreterSpec extends AbstractFreeSkunkDBSpec {

  trait SetUp {
    val testDBConfig: SkunkDBConfig = TestDB.skunkDBConfig
    type R = DBQueryIOStack
    implicit val dbQueryIOInterpreter: SkunkDBQueryIOInterpreter = new SkunkDBQueryIOInterpreter(
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
          SkunkDBQueryIOEffect.withDBSession[R, Option[Pet]] { session =>
            session.prepare(selectPetByName).flatMap(pq => pq.option(pet.name))
          }

        val result: Either[Throwable, Option[Pet]] = await(
          effects.runDBQueryIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        result mustBe Right(Some(pet))
      }
      "Catch DBQueryIOError about cannot command in read-only session" in new SetUp {
        val pet: Pet = Pet.randomGen

        val effects: Eff[R, Unit] =
          SkunkDBQueryIOEffect.withDBSession[R, Unit] { session =>
            for {
              _ <- session.prepare(insertPet()).flatMap(pc => pc.execute(pet))
            } yield ()
          }

        val result: Either[Throwable, Unit] = await(
          effects.runDBQueryIO
            .runEither[Throwable]
            .unsafeToFuture
        )

        result match {
          case Left(DBQueryIOError(e)) => {
            e.getMessage.contains("read-only") mustBe true
          }
          case _                       => fail
        }
      }
    }
  }
}
