package io.github.boykush.eff.dbio.dbSessionIO

trait DBSession[T] {
  val value: T
}
