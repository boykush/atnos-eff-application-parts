package io.github.boykush.eff.addon.skunk

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.unsafe.implicits.global
import io.github.boykush.eff.addon.skunk.AbstractFreeSkunkDBSpec.Pet
import org.scalatest.BeforeAndAfterEach
import natchez.Trace.Implicits.noop
import skunk._
import skunk.codec.all._
import skunk.implicits._

import java.util.UUID
import scala.util.Random

trait AbstractFreeSkunkDBSpec extends AbstractFreeSpec with BeforeAndAfterEach { self =>

  private val tableNamePrefix: String = self.getClass.getSimpleName
  private val tableName: String       = tableNamePrefix + "_pets"

  private val config: SkunkDBConfig                      = TestDB.skunkDBConfig
  private val sessionResource: Resource[IO, Session[IO]] = Session
    .single[IO](
      host = config.host,
      port = config.port,
      user = config.user,
      database = config.database,
      password = Some(config.password)
    )

  /** Direct use of skunk session */
  def sessionUse[A](f: Session[IO] => IO[A]): IO[A] =
    sessionResource.use(f)

  override protected def beforeEach(): Unit = {
    val createPetTable: Command[Void] =
      sql"CREATE TABLE IF NOT EXISTS #$tableName (name varchar unique, age int2)".command

    val io: IO[Unit] =
      sessionUse(session => session.execute(createPetTable).void)

    await(io.unsafeToFuture())
  }

  override protected def afterEach(): Unit = {
    val dropPetTable: Command[Void] =
      sql"DROP TABLE #$tableName".command

    val io: IO[Unit] =
      sessionUse(session => session.execute(dropPetTable).void)

    await(io.unsafeToFuture())
  }

  def insertPet(): Command[Pet] =
    sql"""
          INSERT INTO #$tableName VALUES ($varchar, $int2);
       """.command.gcontramap[Pet]

  def selectPetByName: Query[String, Pet] =
    sql"""
          SELECT name, age FROM #$tableName WHERE name = $varchar;
       """
      .query(varchar ~ int2)
      .gmap[Pet]
}

object AbstractFreeSkunkDBSpec {
  case class Pet(name: String, age: Short)
  object Pet {
    def randomGen: Pet = Pet(name = UUID.randomUUID().toString, age = Random.between(0, 20).toShort)
  }
}
