/*
 * COPYRIGHT (C) FINESWAP.COM AND OTHERS. ALL RIGHTS RESERVED.
 * UNAUTHORIZED DUPLICATION, MODIFICATION OR PUBLICATION IS PROHIBITED.
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF FINESWAP.COM.
 * THE COPYRIGHT NOTICE ABOVE DOES NOT EVIDENCE ANY ACTUAL OR
 * INTENDED PUBLICATION OF SUCH SOURCE CODE.
 */

package org.noordawod.kotlin.orm.dao

import com.j256.ormlite.stmt.PreparedQuery
import com.j256.ormlite.support.ConnectionSource
import org.noordawod.kotlin.core.extension.mutableMapWith
import org.noordawod.kotlin.orm.BaseDatabase.Companion.INITIAL_CAPACITY
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
  dataClass: Class<T>
) : BaseDaoImpl<ID, T>(connection, dataClass) {
  /**
   * Returns the field name serving as the primary key of the catalog.
   */
  abstract val primaryKey: String

  /**
   * Returns a random [ID] suitable as value to [primaryKey].
   */
  abstract fun randomId(entity: T): ID

  /**
   * Optionally populate this entity with fresh details from the database if needed, does
   * nothing otherwise. Will return the populated entity on success, null otherwise.
   */
  open fun populateIfNeeded(entity: T?, force: Boolean = false): T? =
    if (null == entity) {
      null
    } else {
      if (!force && entity.populated) {
        entity
      } else {
        queryFor(entity)?.also { it.populated = true }
      }
    }

  override fun delete(data: T): Int = deleteById(data.id)

  /**
   * Creates a new [entity] in the database and optionally tries the specified number
   * of times to set the correct insert ID in it before failing. The method will always return
   * the stored data obtained from the database.
   */
  @Throws(java.sql.SQLException::class)
  override fun insert(entity: T, tries: Int): T {
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
      "Unable to insert a new record of type ${entity::javaClass.name} to database."
    )
  }

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
    val results = ArrayList<T>(INITIAL_CAPACITY)
    for (instance in instances) {
      if (null == queryForId(instance.id)) {
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
    insert(entity)
  } else {
    update(entity)
    entity
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

  override fun queryForId(id: ID): T? =
    super.queryForId(id)?.also { it.populated = true }

  override fun queryForAll(): List<T>? {
    val result = super.queryForAll()
    return if (result.isNullOrEmpty()) null else result.onEach { it.populated = true }
  }

  override fun queryForEq(fieldName: String, value: Any?): List<T>? {
    val result = super.queryForEq(fieldName, value)
    return if (result.isNullOrEmpty()) null else result.onEach { it.populated = true }
  }

  override fun queryForFirst(preparedQuery: PreparedQuery<T>): T? =
    super.queryForFirst(preparedQuery)?.also { it.populated = true }

  override fun queryForFieldValues(fieldValues: MutableMap<String, Any>): List<T>? {
    val result = super.queryForFieldValues(fieldValues)
    return if (result.isNullOrEmpty()) null else result.onEach { it.populated = true }
  }

  override fun queryForFieldValuesArgs(fieldValues: MutableMap<String, Any>): List<T>? {
    val result = super.queryForFieldValuesArgs(fieldValues)
    return if (result.isNullOrEmpty()) null else result.onEach { it.populated = true }
  }

  override fun queryForMatching(matchObj: T): List<T>? {
    val result = super.queryForMatching(matchObj)
    return if (result.isNullOrEmpty()) null else result.onEach { it.populated = true }
  }

  override fun queryForMatchingArgs(matchObj: T): List<T>? {
    val result = super.queryForMatchingArgs(matchObj)
    return if (result.isNullOrEmpty()) null else result.onEach { it.populated = true }
  }

  override fun queryForSameId(data: T?): T? =
    super.queryForSameId(data)?.also { it.populated = true }

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
  fun queryForFirst(fieldName: String, value: Any?): T? =
    queryForEq(fieldName, value)?.firstOrNull()

  /**
   * Queries the database for distinct records having the specified [identifiers][ids]
   * equal to [primaryKey] field.
   */
  @Throws(java.sql.SQLException::class)
  open fun queryForIds(ids: Collection<ID>): List<T>? =
    queryForIds(primaryKey, ids)?.onEach { it.populated = true }

  /**
   * Queries the database for the specified [identifiers][ids] and sorts the result
   * ascending by the specified sort order field.
   */
  @Throws(java.sql.SQLException::class)
  open fun queryForIds(ids: Collection<ID>, orderField: String): List<T>? =
    queryForIds(ids, 0, orderField, true)

  /**
   * Queries the database for the specified [identifiers][ids] and sorts the result
   * according to the specified sort order.
   */
  @Throws(java.sql.SQLException::class)
  open fun queryForIds(
    ids: Collection<ID>,
    limit: Long = 0,
    orderField: String,
    ascending: Boolean
  ): List<T>? =
    queryForIds(primaryKey, ids, limit, orderField, ascending)?.onEach { it.populated = true }

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
  open fun associateForIds(ids: Collection<ID>, orderField: String): Map<ID, T>? =
    associateForIds(ids, 0, orderField, true)

  /**
   * Queries the database for the specified [identifiers][ids] and sorts the result
   * according to the specified sort order.
   */
  @Throws(java.sql.SQLException::class)
  open fun associateForIds(
    ids: Collection<ID>,
    limit: Long = 0,
    orderField: String,
    ascending: Boolean
  ): Map<ID, T>? = queryForIds(primaryKey, ids, limit, orderField, ascending).toMap()

  /**
   * Converts a list of [T] instances to a Map of keys of [T] and the instance itself.
   */
  open fun Collection<T>?.toMap(): Map<ID, T>? =
    this?.let { instances ->
      mutableMapWith<ID, T>(this.size).apply {
        for (instance in instances) {
          this[instance.id] = instance
        }
      }
    }
}
