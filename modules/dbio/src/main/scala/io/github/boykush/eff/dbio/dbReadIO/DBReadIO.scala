package io.github.boykush.eff.dbio.dbReadIO

import io.github.boykush.eff.dbio.DBSessionF

sealed trait DBReadIO[+A]

case class WithDBSession[T](f: DBSessionF[T]) extends DBReadIO[T]
