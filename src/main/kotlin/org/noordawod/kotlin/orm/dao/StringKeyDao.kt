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
import org.noordawod.kotlin.core.extension.mutableMapWith
import org.noordawod.kotlin.orm.entity.StringKeyEntity

/**
 * All DAOs with a [String] primary ID must extend this class.
 */
abstract class StringKeyDao<T : StringKeyEntity> protected constructor(
  connection: ConnectionSource,
  dataClass: Class<T>,
) : BaseKeyDao<String, T>(connection, dataClass) {
  override fun Collection<T>?.toMap(): Map<String, T>? = if (null == this) {
    null
  } else {
    val map = mutableMapWith<String, T>(size)
    for (instance in this) {
      map[instance.id] = instance
    }

    map
  }

  /**
   * Fetches the row associated with the supplied [id].
   */
  @Throws(java.sql.SQLException::class)
  override fun queryForId(id: String): T? {
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
