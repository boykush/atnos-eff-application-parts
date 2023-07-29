package io.github.boykush.eff.dbio.dbTransactionIO

trait DBTransactionIOError extends Throwable

object DBTransactionIOError {
  case class DatabaseError(e: Throwable) extends DBTransactionIOError

  case object ResourceNotFoundError extends DBTransactionIOError

}
