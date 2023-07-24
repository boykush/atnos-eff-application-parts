package io.github.boykush.eff.dbio.dbReadIO

case class DBReadIOError(e: Throwable) extends Throwable {
  override def toString: String = e.toString
}
