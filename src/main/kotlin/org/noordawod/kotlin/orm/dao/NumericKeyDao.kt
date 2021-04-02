/*
 * COPYRIGHT (C) FINESWAP.COM AND OTHERS. ALL RIGHTS RESERVED.
 * UNAUTHORIZED DUPLICATION, MODIFICATION OR PUBLICATION IS PROHIBITED.
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF FINESWAP.COM.
 * THE COPYRIGHT NOTICE ABOVE DOES NOT EVIDENCE ANY ACTUAL OR
 * INTENDED PUBLICATION OF SUCH SOURCE CODE.
 */

@file:Suppress("unused")

package org.noordawod.kotlin.orm.dao

import com.j256.ormlite.support.ConnectionSource
import org.noordawod.kotlin.orm.entity.BaseKeyEntity
import org.noordawod.kotlin.orm.entity.PublicId

/**
 * All DAOs with a primary ID of type [Number] must extend this class.
 */
abstract class NumericKeyDao<ID : Number, T : BaseKeyEntity<ID>> protected constructor(
  connection: ConnectionSource,
  dataClass: Class<T>
) : BaseKeyDao<ID, T>(connection, dataClass) {
  override fun publicId(id: ID): PublicId = id.toString()

  /**
   * Returns the primary ID of the last insert operation.
   */
  @Throws(java.sql.SQLException::class)
  open fun insertId(): Long = try {
    queryRawValue("SELECT LAST_INSERT_ID()")
  } catch (ignored: java.sql.SQLException) {
    0L
  }
}
