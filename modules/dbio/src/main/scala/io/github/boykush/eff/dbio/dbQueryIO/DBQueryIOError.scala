package io.github.boykush.eff.dbio.dbQueryIO

case class DBQueryIOError(e: Throwable) extends Throwable {
  override def toString: String = e.toString
}
