package io.github.boykush.eff.dbio.dbTransactionIO

import io.github.boykush.eff.dbio.DBSessionF

sealed trait DBTransactionIO[+A]

case class WithDBSession[T](f: DBSessionF[T]) extends DBTransactionIO[T]
