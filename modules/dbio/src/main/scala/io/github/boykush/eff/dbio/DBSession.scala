package io.github.boykush.eff.dbio

trait DBSession[T] {
  val value: T
}
