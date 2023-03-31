package io.github.boykush.eff.dbio.dbCommandIO

import org.atnos.eff.|=

trait DBCommandIOTypes {
  type _dbCommandIO[R] = DBCommandIO |= R
}

object DBCommandIOTypes extends DBCommandIOTypes
