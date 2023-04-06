package io.github.boykush.eff.dbio.dbCommandIO

case class DBCommandIOError(e: Throwable) extends Throwable {
  override def toString: String = e.toString
}
