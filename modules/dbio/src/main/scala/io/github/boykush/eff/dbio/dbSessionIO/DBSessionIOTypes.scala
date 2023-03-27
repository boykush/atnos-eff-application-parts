package io.github.boykush.eff.dbio.dbSessionIO

import org.atnos.eff.|=

trait DBSessionIOTypes {
  type _dbSessionIO[R] = DBSessionIO |= R
}

object DBSessionIOTypes extends DBSessionIOTypes
