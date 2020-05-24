/*
 * COPYRIGHT (C) FINESWAP.COM AND OTHERS. ALL RIGHTS RESERVED.
 * UNAUTHORIZED DUPLICATION, MODIFICATION OR PUBLICATION IS PROHIBITED.
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF FINESWAP.COM.
 * THE COPYRIGHT NOTICE ABOVE DOES NOT EVIDENCE ANY ACTUAL OR
 * INTENDED PUBLICATION OF SUCH SOURCE CODE.
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.noordawod.kotlin.orm.dao

import com.j256.ormlite.support.ConnectionSource
import org.noordawod.kotlin.orm.entity.ByteArrayKeyEntity

/**
 * All DAOs with a [ByteArray] primary ID and a backing model must extend this class.
 */
abstract class ModelByteArrayKeyDao<T : ByteArrayKeyEntity, M> protected constructor(
  connection: ConnectionSource,
  dataClass: Class<T>
) : ByteArrayKeyDao<T>(connection, dataClass) {
  /**
   * Converts [model] to an entity of type [T].
   */
  abstract fun fromModel(model: M): T

  /**
   * Converts [entity] to a model of type [M].
   */
  abstract fun toModel(entity: T): M
}
