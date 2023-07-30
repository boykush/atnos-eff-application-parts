package io.github.boykush.eff.dbio.dbReadIO

case class Pager(limit: Int, offset: Int)

object Pager {
  def all: Pager = Pager(Int.MaxValue, 0)
}
