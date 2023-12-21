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

import com.j256.ormlite.stmt.PreparedQuery
import com.j256.ormlite.support.ConnectionSource
import org.noordawod.kotlin.core.Constants
import org.noordawod.kotlin.core.extension.mutableMapWith
import org.noordawod.kotlin.orm.entity.BaseKeyEntity

/**
 * All DAO instances which have primary keys must extend this base class.
 *
 * @param ID type of entity key this DAO is managing
 * @param T type of model this DAO is managing
 */
@Suppress("TooManyFunctions")
abstract class BaseKeyDao<ID, T : BaseKeyEntity<ID>> protected constructor(
  connection: ConnectionSource,
  dataClass: Class<T>,
) : BaseDaoImpl<ID, T>(connection, dataClass) {
  /**
   * Returns the field name serving as the primary key of the catalog.
   */
  abstract val primaryKey: String

  /**
   * Optionally populate this entity with fresh details from the database if needed, does
   * nothing otherwise. Will return the populated entity on success, null otherwise.
   */
  open fun populateIfNeeded(
    entity: T?,
    force: Boolean = false,
  ): T? = when {
    null == entity -> null
    !force && entity.populated -> entity
    else -> {
      val entityUpdated = queryFor(entity)
      entityUpdated?.populated = true
      entityUpdated
    }
  }

  override fun delete(data: T): Int = deleteById(data.id)

  /**
   * Inserts an ORM object into the database and returns either the new or existing
   * record as the result.
   */
  @Throws(java.sql.SQLException::class)
  open fun insertIfNew(entity: T): T = queryForId(entity.id) ?: insert(entity)

  /**
   * Insert new ORM instances into the database if they're new, returns the ones that
   * were actually inserted.
   */
  @Throws(java.sql.SQLException::class)
  open fun insertIfNew(instances: Collection<T>): List<T>? {
    val results = ArrayList<T>(Constants.DEFAULT_LIST_CAPACITY)
    for (instance in instances) {
      if (!exists(instance.id)) {
        results.add(insert(instance))
      }
    }

    return if (results.isEmpty()) null else results
  }

  /**
   * Updates a record in the database if it exists or inserts it as new, always returns
   * the same record that was updated or inserted.
   */
  @Throws(java.sql.SQLException::class)
  open fun replace(entity: T): T = if (exists(entity.id)) {
    update(entity)
    entity
  } else {
    insert(entity)
  }

  /**
   * Returns true if the provided [id] is already in database, false otherwise.
   */
  @Throws(java.sql.SQLException::class)
  open fun exists(id: ID): Boolean = null != queryForId(id)

  /**
   * Returns true if all provided [ids] are already in database, false otherwise.
   */
  @Throws(java.sql.SQLException::class)
  open fun exists(ids: Collection<ID>): Boolean = ids.size == queryForIds(ids)?.size

  override fun queryForId(id: ID): T? {
    val result = super.queryForId(id)
    result?.populated = true

    return result
  }

  override fun queryForAll(): List<T>? {
    val result = super.queryForAll()

    return if (result.isNullOrEmpty()) {
      null
    } else {
      result.onEach {
        it.populated = true
      }
    }
  }

  override fun queryForEq(
    fieldName: String,
    value: Any?,
  ): List<T>? {
    val result = super.queryForEq(fieldName, value)

    return if (result.isNullOrEmpty()) {
      null
    } else {
      result.onEach {
        it.populated = true
      }
    }
  }

  override fun queryForFirst(preparedQuery: PreparedQuery<T>): T? {
    val result = super.queryForFirst(preparedQuery)
    result?.populated = true

    return result
  }

  override fun queryForFieldValues(fieldValues: Map<String, Any?>): List<T>? {
    val result = super.queryForFieldValues(fieldValues)

    return if (result.isNullOrEmpty()) {
      null
    } else {
      result.onEach {
        it.populated = true
      }
    }
  }

  override fun queryForFieldValuesArgs(fieldValues: Map<String, Any?>): List<T>? {
    val result = super.queryForFieldValuesArgs(fieldValues)

    return if (result.isNullOrEmpty()) {
      null
    } else {
      result.onEach {
        it.populated = true
      }
    }
  }

  override fun queryForMatching(matchObj: T): List<T>? {
    val result = super.queryForMatching(matchObj)

    return if (result.isNullOrEmpty()) {
      null
    } else {
      result.onEach {
        it.populated = true
      }
    }
  }

  override fun queryForMatchingArgs(matchObj: T): List<T>? {
    val result = super.queryForMatchingArgs(matchObj)

    return if (result.isNullOrEmpty()) {
      null
    } else {
      result.onEach {
        it.populated = true
      }
    }
  }

  override fun queryForSameId(data: T?): T? {
    val result = super.queryForSameId(data)
    result?.populated = true

    return result
  }

  /**
   * Queries the database for the single record matching the provided [instance].
   */
  @Throws(java.sql.SQLException::class)
  open fun queryFor(instance: T): T? = queryForId(instance.id)

  /**
   * Queries the database for the first record with a [fieldName] equal to [value],
   * null otherwise.
   */
  @Throws(java.sql.SQLException::class)
  fun queryForFirst(
    fieldName: String,
    value: Any?,
  ): T? = queryForEq(
    fieldName = fieldName,
    value = value,
  )?.firstOrNull()

  /**
   * Queries the database for distinct records having the specified [identifiers][ids]
   * equal to [primaryKey] field.
   */
  @Throws(java.sql.SQLException::class)
  open fun queryForIds(ids: Collection<ID>): List<T>? = queryForIds(
    fieldName = primaryKey,
    ids = ids,
  )?.onEach {
    it.populated = true
  }

  /**
   * Queries the database for the specified [identifiers][ids] and sorts the result
   * ascending by the specified sort order field.
   */
  @Throws(java.sql.SQLException::class)
  open fun queryForIds(
    ids: Collection<ID>,
    orderField: String,
  ): List<T>? = queryForIds(
    ids = ids,
    limit = 0,
    orderField = orderField,
    ascending = true,
  )

  /**
   * Queries the database for the specified [identifiers][ids] and sorts the result
   * according to the specified sort order.
   */
  @Throws(java.sql.SQLException::class)
  open fun queryForIds(
    ids: Collection<ID>,
    limit: Long = 0,
    orderField: String,
    ascending: Boolean,
  ): List<T>? = queryForIds(
    fieldName = primaryKey,
    ids = ids,
    limit = limit,
    orderField = orderField,
    ascending = ascending,
  )?.onEach {
    it.populated = true
  }

  /**
   * Queries the database for distinct records having the specified [identifiers][ids]
   * equal to [primaryKey] field, and then returns a Map of IDs and their instances.
   */
  @Throws(java.sql.SQLException::class)
  open fun associateForIds(ids: Collection<ID>): Map<ID, T>? = queryForIds(ids).toMap()

  /**
   * Queries the database for the specified [identifiers][ids] and sorts the result
   * ascending by the specified sort order field.
   */
  @Throws(java.sql.SQLException::class)
  open fun associateForIds(
    ids: Collection<ID>,
    orderField: String,
  ): Map<ID, T>? = associateForIds(
    ids = ids,
    limit = 0,
    orderField = orderField,
    ascending = true,
  )

  /**
   * Queries the database for the specified [identifiers][ids] and sorts the result
   * according to the specified sort order.
   */
  @Throws(java.sql.SQLException::class)
  open fun associateForIds(
    ids: Collection<ID>,
    limit: Long = 0,
    orderField: String,
    ascending: Boolean,
  ): Map<ID, T>? = queryForIds(
    fieldName = primaryKey,
    ids = ids,
    limit = limit,
    orderField = orderField,
    ascending = ascending,
  ).toMap()

  /**
   * Converts a list of [T] instances to a Map of keys of [T] and the instance itself.
   */
  open fun Collection<T>?.toMap(): Map<ID, T>? = if (null == this) {
    null
  } else {
    val map = mutableMapWith<ID, T>(this.size)
    for (instance in this) {
      map[instance.id] = instance
    }

    map
  }
}
