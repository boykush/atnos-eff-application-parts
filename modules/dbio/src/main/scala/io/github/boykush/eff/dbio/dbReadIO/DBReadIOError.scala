package io.github.boykush.eff.dbio.dbReadIO

trait DBReadIOError extends Throwable

object DBReadIOError {
  case class DatabaseError(e: Throwable) extends DBReadIOError
}
