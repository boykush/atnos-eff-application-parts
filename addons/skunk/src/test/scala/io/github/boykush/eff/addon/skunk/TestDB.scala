package io.github.boykush.eff.addon.skunk

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object TestDB {
  val config: Config = ConfigFactory.load()

  val skunkDBConfig: SkunkDBConfig = SkunkDBConfig(
    host = config.getString("db.config.host"),
    port = config.getInt("db.config.port"),
    user = config.getString("db.config.user"),
    password = config.getString("db.config.password"),
    database = config.getString("db.config.database"),
    maxConnections = config.getInt("db.config.maxConnections")
  )
}
