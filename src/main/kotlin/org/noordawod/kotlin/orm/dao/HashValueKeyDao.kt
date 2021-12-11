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
import org.noordawod.kotlin.core.util.ByteArrayMap
import org.noordawod.kotlin.orm.entity.HashValue
import org.noordawod.kotlin.orm.entity.HashValueKeyEntity
import org.noordawod.kotlin.orm.entity.PublicId
import org.noordawod.kotlin.security.base62

/**
 * All DAOs with a [HashValue] primary ID must extend this class.
 */
abstract class HashValueKeyDao<T : HashValueKeyEntity> protected constructor(
  connection: ConnectionSource,
  dataClass: Class<T>
) : BaseKeyDao<HashValue, T>(connection, dataClass) {
  fun publicId(id: HashValue): PublicId = id.base62()

  fun internalId(id: PublicId): HashValue = id.base62()

  override fun Collection<T>?.toMap(): ByteArrayMap<T>? =
    this?.let { instances ->
      ByteArrayMap<T>().apply {
        for (instance in instances) {
          this[instance.id] = instance
        }
      }
    }

  /**
   * Fetches the row associated with the supplied [id].
   */
  @Throws(java.sql.SQLException::class)
  override fun queryForId(id: HashValue): T? = queryBuilder()
    .where()
    .eq(primaryKey, id)
    .queryForFirst()
    ?.also { it.populated = true }
}
