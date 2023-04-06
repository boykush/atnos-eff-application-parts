package io.github.boykush.eff.addon.skunk

import io.github.boykush.eff.addon.skunk.SkunkTestPetService.Pet
import skunk._
import skunk.codec.all._
import skunk.implicits._

import java.util.UUID
import scala.util.Random

class SkunkTestPetService(tableNamePrefix: String) {
  val tableName: String = tableNamePrefix + "_pets"

  def createTable: Command[Void] =
    sql"CREATE TABLE IF NOT EXISTS #$tableName (name varchar unique, age int2)".command

  def insert: Command[Pet] =
    sql"""
          INSERT INTO #$tableName VALUES ($varchar, $int2);
       """.command.gcontramap[Pet]

  def selectByName: Query[String, Pet] =
    sql"""
          SELECT name, age FROM #$tableName WHERE name = $varchar;
       """
      .query(varchar ~ int2)
      .gmap[Pet]
}

object SkunkTestPetService {
  case class Pet(name: String, age: Short)
  object Pet {
    def randomGen: Pet = Pet(name = UUID.randomUUID().toString, age = Random.between(0, 20).toShort)
  }
}
