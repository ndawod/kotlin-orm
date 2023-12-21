/*
 * The MIT License
 *
 * Copyright 2022 Noor Dawod. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

@file:Suppress("unused")

package org.noordawod.kotlin.orm.dao

import com.j256.ormlite.support.ConnectionSource
import org.noordawod.kotlin.core.repository.HashValue
import org.noordawod.kotlin.core.util.ByteArrayMap
import org.noordawod.kotlin.orm.entity.HashValueKeyEntity

/**
 * All DAOs with a [HashValue] primary ID must extend this class.
 */
abstract class HashValueKeyDao<T : HashValueKeyEntity> protected constructor(
  connection: ConnectionSource,
  dataClass: Class<T>,
) : BaseKeyDao<HashValue, T>(connection, dataClass) {
  /**
   * Returns a random [HashValue] suitable as value to [primaryKey].
   */
  abstract fun randomId(entity: T): HashValue

  /**
   * Creates a new [entity] in the database and optionally tries the specified number
   * of times to set the correct insert ID in it before failing. The method will always return
   * the stored data obtained from the database.
   */
  @Throws(java.sql.SQLException::class)
  override fun insert(
    entity: T,
    tries: Int,
  ): T {
    var thisTry = tries
    var lastError: java.sql.SQLException?
    do {
      try {
        entity.id = randomId(entity)
        super.create(entity)
        return queryForId(entity.id)
          ?: throw java.sql.SQLException("Unable to insert row after $tries tries.")
      } catch (e: java.sql.SQLException) {
        lastError = e
        thisTry--
      }
    } while (0 < thisTry)

    throw lastError ?: java.sql.SQLException(
      "Unable to insert a new record of type ${entity::javaClass.name} to database.",
    )
  }

  override fun Collection<T>?.toMap(): ByteArrayMap<T>? = if (null == this) {
    null
  } else {
    val map = ByteArrayMap<T>()
    for (instance in this) {
      map[instance.id] = instance
    }

    map
  }

  /**
   * Fetches the row associated with the supplied [id].
   */
  @Throws(java.sql.SQLException::class)
  override fun queryForId(id: HashValue): T? {
    val result = queryBuilder()
      .where()
      .eq(primaryKey, id)
      .queryForFirst()

    if (null != result) {
      result.populated = true
    }

    return result
  }
}
