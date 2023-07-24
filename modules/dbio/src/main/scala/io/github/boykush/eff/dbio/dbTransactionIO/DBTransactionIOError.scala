package io.github.boykush.eff.dbio.dbTransactionIO

case class DBTransactionIOError(e: Throwable) extends Throwable {
  override def toString: String = e.toString
}
