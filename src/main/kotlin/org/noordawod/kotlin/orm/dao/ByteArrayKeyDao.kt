/*
 * COPYRIGHT (C) FINESWAP.COM AND OTHERS. ALL RIGHTS RESERVED.
 * UNAUTHORIZED DUPLICATION, MODIFICATION OR PUBLICATION IS PROHIBITED.
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF FINESWAP.COM.
 * THE COPYRIGHT NOTICE ABOVE DOES NOT EVIDENCE ANY ACTUAL OR
 * INTENDED PUBLICATION OF SUCH SOURCE CODE.
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package org.noordawod.kotlin.orm.dao

import com.j256.ormlite.support.ConnectionSource
import org.noordawod.kotlin.core.util.ByteArrayMap
import org.noordawod.kotlin.orm.entity.ByteArrayKeyEntity
import org.noordawod.kotlin.orm.entity.PublicId
import org.noordawod.kotlin.security.base62

/**
 * All DAOs with a [ByteArray] primary ID must extend this class.
 */
abstract class ByteArrayKeyDao<T : ByteArrayKeyEntity> protected constructor(
  connection: ConnectionSource,
  dataClass: Class<T>
) : BaseKeyDao<ByteArray, T>(connection, dataClass) {
  override fun publicId(id: ByteArray): PublicId = id.base62()

  override fun internalId(id: PublicId): ByteArray = id.base62()

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
  override fun queryForId(id: ByteArray): T? = queryBuilder()
    .where()
    .eq(primaryKey, id)
    .queryForFirst()
}
