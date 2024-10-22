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

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.noordawod.kotlin.orm.dao

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.support.ConnectionSource
import org.noordawod.kotlin.core.Constants
import org.noordawod.kotlin.orm.BaseDatabase

/**
 * Base DAO for all others to extend from. Includes helper methods for all DAOs.
 */
@Suppress("TooManyFunctions")
abstract class BaseDaoImpl<ID, T> protected constructor(
  connection: ConnectionSource,
  dataClass: Class<T>,
) : com.j256.ormlite.dao.BaseDaoImpl<T, ID>(connection, dataClass),
  Dao<T, ID> {
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
    ascending: Boolean = true,
  ): List<T>? {
    val builder = queryBuilder()
      .distinct()
      .orderBy(orderField, ascending)

    if (0 < limit) {
      builder.limit(limit)
    }

    val result = builder
      .where()
      .eq(fieldName, id)
      .query()

    return if (result.isNullOrEmpty()) null else result
  }

  /**
   * Queries the database for distinct records having the specified ID equal to the
   * specified field.
   */
  @Throws(java.sql.SQLException::class)
  fun queryForId(
    fieldName: String,
    id: ID,
  ): List<T>? {
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
    ascending: Boolean = true,
  ): List<T>? = queryForIds(
    fieldName = fieldName,
    ids = ids,
    limit = 0,
    orderField = orderField,
    ascending = ascending,
  )

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
    ascending: Boolean = true,
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
  fun queryForIds(
    fieldName: String,
    ids: Collection<ID>,
  ): List<T>? {
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
    replaceIntoTable(
      targetTableName = targetTableName,
      whereClause = null,
    )
  }

  /**
   * Copies some rows from this table to another one using MySQL's quick
   * "REPLACE INTO … SELECT FROM" SQL command.
   */
  @Throws(java.sql.SQLException::class)
  fun replaceIntoTable(
    targetTableName: String,
    whereClause: String?,
    args: Array<String>? = null,
  ) {
    val builder = StringBuilder("REPLACE INTO ")
    val dotPos = targetTableName.indexOf('.')
    if (0 < dotPos) {
      databaseType.appendEscapedEntityName(
        builder,
        targetTableName.substring(0, dotPos),
      )
      builder.append('.')
      databaseType.appendEscapedEntityName(
        builder,
        targetTableName.substring(1 + dotPos),
      )
    } else {
      databaseType.appendEscapedEntityName(
        builder,
        targetTableName,
      )
    }
    performOperation(
      builder = builder,
      initialCommand = " SELECT * FROM ",
      whereClause = whereClause,
      args = args,
    )
  }

  /**
   * Copies the entire table from this table to another one using MySQL's quick
   * "REPLACE INTO … SELECT FROM" SQL command, and then removes the source rows from
   * this table.
   */
  @Throws(java.sql.SQLException::class)
  fun moveIntoDatabase(targetTableName: String) {
    moveIntoTable(
      targetTableName = targetTableName,
      whereClause = null,
    )
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
    args: Array<String>? = null,
  ) {
    replaceIntoTable(
      targetTableName = targetTableName,
      whereClause = whereClause,
      args = args,
    )
    performOperation(
      builder = StringBuilder(),
      initialCommand = "DELETE FROM ",
      whereClause = whereClause,
      args = args,
    )
  }

  /**
   * Returns the maximum value for a field inside this model.
   */
  @Throws(java.sql.SQLException::class)
  open fun getNextSorting(
    sortingKey: String,
    primaryKey: String,
    keyValue: Any,
  ): T? = queryBuilder()
    .orderBy(sortingKey, false)
    .limit(1L)
    .where()
    .eq(primaryKey, keyValue)
    .queryForFirst()

  /**
   * Creates a new [entity] in the database.
   */
  @Throws(java.sql.SQLException::class)
  fun insert(entity: T): T = insert(
    entity = entity,
    tries = BaseDatabase.DEFAULT_INSERT_TRIES,
  )

  /**
   * Creates a new [entity] in the database and optionally tries the specified number
   * of times to set the correct insert ID in it before failing.
   */
  @Throws(java.sql.SQLException::class)
  open fun insert(
    entity: T,
    tries: Int = BaseDatabase.DEFAULT_INSERT_TRIES,
  ): T {
    create(entity)

    return entity
  }

  /**
   * Creates new [entities] in the database.
   */
  @Throws(java.sql.SQLException::class)
  fun insert(entities: Collection<T>): List<T> = insert(
    entities = entities,
    tries = BaseDatabase.DEFAULT_INSERT_TRIES,
  )

  /**
   * Creates new [entities] in the database and optionally tries the specified number
   * of times to set the correct insert ID in each before failing.
   */
  @Throws(java.sql.SQLException::class)
  open fun insert(
    entities: Collection<T>,
    tries: Int = BaseDatabase.DEFAULT_INSERT_TRIES,
  ): List<T> {
    val results = ArrayList<T>(Constants.DEFAULT_LIST_CAPACITY)
    for (entry in entities) {
      results.add(
        insert(
          entity = entry,
          tries = tries,
        ),
      )
    }

    return results
  }

  @Suppress("SpreadOperator")
  private fun performOperation(
    builder: StringBuilder,
    initialCommand: String,
    whereClause: String?,
    args: Array<String>?,
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
}
