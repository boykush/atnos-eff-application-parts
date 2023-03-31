package io.github.boykush.eff.dbio.dbQueryIO

import org.atnos.eff.|=

trait DBQueryIOTypes {
  type _dbQueryIO[R] = DBQueryIO |= R
}

object DBQueryIOTypes extends DBQueryIOTypes
