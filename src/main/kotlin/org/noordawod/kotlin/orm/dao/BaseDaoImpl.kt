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

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.support.ConnectionSource
import org.noordawod.kotlin.orm.BaseDatabase.Companion.INITIAL_CAPACITY
import org.noordawod.kotlin.orm.BaseDatabase.Companion.MAX_TRIES
import org.noordawod.kotlin.orm.entity.PublicId

/**
 * Base DAO for all others to extend from. Includes helper methods for all DAOs.
 */
@Suppress("TooManyFunctions")
abstract class BaseDaoImpl<ID, T> protected constructor(
  connection: ConnectionSource,
  dataClass: Class<T>
) : com.j256.ormlite.dao.BaseDaoImpl<T, ID>(connection, dataClass), Dao<T, ID> {
  /**
   * Returns the database name this DAO is currently is attached to.
   */
  @get:Throws(java.sql.SQLException::class)
  val catalogName: String? get() = getCatalogName(this)

  /**
   * Returns the public identifier for the provided [id].
   */
  abstract fun publicId(id: ID): PublicId

  /**
   * Returns the internal identifier for the provided public [id].
   */
  abstract fun internalId(id: PublicId): ID

  /**
   * Queries the database for distinct records having the specified ID equal to the
   * specified field.
   */
  @Throws(java.sql.SQLException::class)
  fun queryForId(
    fieldName: String,
    id: ID,
    limit: Long = 0,
    orderField: String,
    ascending: Boolean = true
  ): List<T>? {
    val builder = queryBuilder().distinct().orderBy(orderField, ascending)
    if (0 < limit) {
      builder.limit(limit)
    }
    val result = builder.where().eq(fieldName, id).query()
    return if (result.isNullOrEmpty()) null else result
  }

  /**
   * Queries the database for distinct records having the specified ID equal to the
   * specified field.
   */
  @Throws(java.sql.SQLException::class)
  fun queryForId(fieldName: String, id: ID): List<T>? {
    val result = queryBuilder()
      .distinct()
      .where()
      .eq(fieldName, id)
      .query()
    return if (result.isNullOrEmpty()) null else result
  }

  /**
   * Queries the database for distinct records having the specified IDs equal to the
   * specified field.
   */
  @Throws(java.sql.SQLException::class)
  fun queryForIds(
    fieldName: String,
    ids: Collection<ID>,
    orderField: String,
    ascending: Boolean = true
  ): List<T>? = queryForIds(fieldName, ids, 0, orderField, ascending)

  /**
   * Queries the database for distinct records having the specified IDs equal to the
   * specified field.
   */
  @Throws(java.sql.SQLException::class)
  fun queryForIds(
    fieldName: String,
    ids: Collection<ID>,
    limit: Long = 0,
    orderField: String,
    ascending: Boolean = true
  ): List<T>? {
    val builder = queryBuilder().distinct().orderBy(orderField, ascending)
    if (0 < limit) {
      builder.limit(limit)
    }
    val result = builder.where().`in`(fieldName, ids).query()
    return if (result.isNullOrEmpty()) null else result
  }

  /**
   * Queries the database for distinct records having the specified IDs equal to the
   * specified field.
   */
  @Throws(java.sql.SQLException::class)
  fun queryForIds(fieldName: String, ids: Collection<ID>): List<T>? {
    val result = queryBuilder()
      .distinct()
      .where()
      .`in`(fieldName, ids)
      .query()
    return if (result.isNullOrEmpty()) null else result
  }

  /**
   * Copies the entire table to another one using MySQL's quick
   * "REPLACE INTO … SELECT FROM" SQL command.
   */
  @Throws(java.sql.SQLException::class)
  fun replaceIntoDatabase(targetTableName: String) {
    replaceIntoTable(targetTableName, null)
  }

  /**
   * Copies some rows from this table to another one using MySQL's quick
   * "REPLACE INTO … SELECT FROM" SQL command.
   */
  @Throws(java.sql.SQLException::class)
  fun replaceIntoTable(
    targetTableName: String,
    whereClause: String?,
    args: Array<String>? = null
  ) {
    val builder = StringBuilder("REPLACE INTO ")
    val dotPos = targetTableName.indexOf('.')
    if (0 < dotPos) {
      databaseType.appendEscapedEntityName(builder, targetTableName.substring(0, dotPos))
      builder.append('.')
      databaseType.appendEscapedEntityName(builder, targetTableName.substring(1 + dotPos))
    } else {
      databaseType.appendEscapedEntityName(builder, targetTableName)
    }
    performOperation(builder, " SELECT * FROM ", whereClause, args)
  }

  /**
   * Copies the entire table from this table to another one using MySQL's quick
   * "REPLACE INTO … SELECT FROM" SQL command, and then removes the source rows from
   * this table.
   */
  @Throws(java.sql.SQLException::class)
  fun moveIntoDatabase(targetTableName: String) {
    moveIntoTable(targetTableName, null)
  }

  /**
   * Copies some rows from this table to another one using MySQL's quick
   * "REPLACE INTO … SELECT FROM" SQL command, and then removes the source rows from
   * this table.
   */
  @Throws(java.sql.SQLException::class)
  fun moveIntoTable(
    targetTableName: String,
    whereClause: String?,
    args: Array<String>? = null
  ) {
    replaceIntoTable(targetTableName, whereClause, args)
    performOperation(StringBuilder(), "DELETE FROM ", whereClause, args)
  }

  /**
   * Returns the maximum value for a field inside this model.
   */
  @Throws(java.sql.SQLException::class)
  open fun getNextSorting(sortingKey: String, primaryKey: String, keyValue: Any): T? =
    queryBuilder()
      .orderBy(sortingKey, false)
      .limit(1L)
      .where()
      .eq(primaryKey, keyValue)
      .queryForFirst()

  /**
   * Creates a new [entity] in the database.
   */
  @Throws(java.sql.SQLException::class)
  fun insert(entity: T): T = insert(entity, MAX_TRIES)

  /**
   * Creates a new [entity] in the database and optionally tries the specified number
   * of times to set the correct insert ID in it before failing.
   */
  @Throws(java.sql.SQLException::class)
  open fun insert(entity: T, tries: Int = MAX_TRIES): T = create(entity).let { entity }

  /**
   * Creates new [entities] in the database.
   */
  @Throws(java.sql.SQLException::class)
  fun insert(entities: Collection<T>): List<T> = insert(entities, MAX_TRIES)

  /**
   * Creates new [entities] in the database and optionally tries the specified number
   * of times to set the correct insert ID in each before failing.
   */
  @Throws(java.sql.SQLException::class)
  open fun insert(entities: Collection<T>, tries: Int = MAX_TRIES): List<T> {
    val results = ArrayList<T>(INITIAL_CAPACITY)
    for (entry in entities) {
      results.add(insert(entry, tries))
    }
    return results
  }

  @Suppress("SpreadOperator")
  private fun performOperation(
    builder: StringBuilder,
    initialCommand: String,
    whereClause: String?,
    args: Array<String>?
  ) {
    builder.append(initialCommand)
    databaseType.appendEscapedEntityName(builder, tableInfo.tableName)
    if (!whereClause.isNullOrBlank()) {
      builder.append(" WHERE ")
      builder.append(whereClause)
    }
    val queryString = builder.toString()
    if (null == args) executeRaw(queryString) else executeRaw(queryString, *args)
  }

  companion object {
    /**
     * Returns the catalog (database) name the specified DAO is currently attached to.
     */
    @Throws(java.sql.SQLException::class)
    fun getCatalogName(connection: com.j256.ormlite.dao.BaseDaoImpl<*, *>): String? {
      val values = connection.queryRaw("SELECT DATABASE()")?.firstResult
      return if (null != values && 1 == values.size) values[0] else null
    }
  }
}
