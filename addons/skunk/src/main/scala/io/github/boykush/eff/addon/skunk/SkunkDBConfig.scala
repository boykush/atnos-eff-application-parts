package io.github.boykush.eff.addon.skunk

case class SkunkDBConfig(
  host: String,
  port: Int,
  user: String,
  database: String,
  password: String,
  maxConnections: Int
)
