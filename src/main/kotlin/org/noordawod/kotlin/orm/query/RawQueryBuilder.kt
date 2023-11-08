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

@file:Suppress("unused", "MemberVisibilityCanBePrivate", "DataClassContainsFunctions")

package org.noordawod.kotlin.orm.query

/**
 * A helper class that facilitates building a raw query.
 *
 * When building a raw query, there are few components that can change as one proceeds to build
 * the query:
 *
 * - The main entities to return (result): these appear after the initial SELECT statement.
 * - The joins: these appear after the list of entities to return.
 * - The where clause: appears after the joins.
 * - order, limits: appear at the end.
 *
 * @param op the logical operator between WHERE conditions
 * @param fieldSeparator the character is used to escape table and entity names
 * @param initialCapacity initial capacity of internal lists
 */
@Suppress("TooManyFunctions")
class RawQueryBuilder(
  val op: LogicalOp,
  val fieldSeparator: Char,
  initialCapacity: Int,
) {
  /**
   * Returns a new [RawQueryBuilder] instance with a default initial capacity.
   *
   * @param fieldSeparator the character is used to escape table and entity names
   */
  constructor(
    op: LogicalOp,
    fieldSeparator: Char,
  ) : this(
    op = op,
    fieldSeparator = fieldSeparator,
    initialCapacity = INITIAL_CAPACITY,
  )

  private val tables = LinkedHashSet<TableSpecification>(initialCapacity)
  private val entities = LinkedHashSet<String>(initialCapacity)
  private val joins = LinkedHashSet<String>(initialCapacity)
  private val conditions = LinkedHashSet<Condition>(initialCapacity)

  private var limitInternal: Int = -1
  private var offsetInternal: Int = -1
  private var groupByInternal: String? = null
  private var orderByInternal: String? = null
  private var ascendingInternal: Boolean = true

  @Suppress("NestedBlockDepth", "ComplexFunction", "CyclomaticComplexMethod")
  override fun toString(): String {
    val result = StringBuilder(MINIMUM_BLOCK_SIZE)

    result.append("SELECT ")
    result.append(entities.joinToString(separator = ","))
    result.append(" FROM ")

    tables.forEach {
      result.append(it.toString(::escape))
    }

    joins.forEach {
      result.append(" $it")
    }

    if (conditions.isNotEmpty()) {
      val where = StringBuilder(MINIMUM_BLOCK_SIZE)

      for (condition in conditions) {
        if (!condition.isValid) {
          continue
        }

        if (where.isNotEmpty()) {
          where.append(" $op ")
        }

        if (condition.parenthesized || null == condition.op || op == condition.op) {
          where.append("$condition")
        } else {
          where.append("($condition)")
        }
      }

      if (where.isNotEmpty()) {
        result.append(" WHERE ($where)")
      }
    }

    if (!groupByInternal.isNullOrEmpty()) {
      result.append(" GROUP BY $groupByInternal")
    }

    if (!orderByInternal.isNullOrEmpty()) {
      result.append(" ORDER BY $orderByInternal ${if (ascendingInternal) "ASC" else "DESC"}")
    }

    if (0 < limitInternal) {
      result.append(" LIMIT $limitInternal")
      if (0 < offsetInternal) {
        result.append(" OFFSET $offsetInternal")
      }
    }

    return result.toString()
  }

  /**
   * Adds a new affected table to the query.
   *
   * @param table table specification
   */
  fun table(table: TableSpecification): RawQueryBuilder {
    tables.add(table)
    return this
  }

  /**
   * Adds a new entity to fetch.
   *
   * @param entity exact name of the entity to fetch, including any table name or alias
   */
  fun select(entity: String): RawQueryBuilder {
    entities.add(entity)
    return this
  }

  /**
   * Adds a new entity prefixed by the specified table to fetch.
   *
   * @param table table specification
   * @param entity the entity name to fetch, without a table name or alias
   */
  fun select(table: TableSpecification, entity: String): RawQueryBuilder {
    select(table.prefix(entity, ::escape))
    return table(table)
  }

  /**
   * Adds all entities inside the specified table to fetch.
   *
   * @param table table specification
   */
  fun selectAll(table: TableSpecification): RawQueryBuilder {
    select(table.prefix("*", ::escape))
    return table(table)
  }

  /**
   * Adds a LEFT JOIN to this query between two tables.
   *
   * @param table the first table
   * @param other other table to join
   */
  fun leftJoin(table: JoinSpecification, other: JoinSpecification): RawQueryBuilder =
    leftJoin(table to other)

  /**
   * Adds a LEFT JOIN to this query between two tables.
   *
   * @param on tables to join
   */
  fun leftJoin(on: JoinPair): RawQueryBuilder = leftJoin(listOf(on))

  /**
   * Adds a LEFT JOIN to this query between multiple tables.
   *
   * @param on collection of join details
   */
  fun leftJoin(on: Collection<JoinPair>): RawQueryBuilder {
    joinInternal("LEFT ", on)
    return this
  }

  /**
   * Adds a new condition to the WHERE part.
   *
   * @param condition condition to add
   */
  fun where(condition: Condition): RawQueryBuilder {
    conditions.add(condition)
    return this
  }

  /**
   * Imposes a limit on how many rows should be returned.
   *
   * @param capacity maximum number of rows to return
   * @param offset position at which to return the rows
   */
  fun limit(capacity: Int, offset: Int = 0): RawQueryBuilder {
    limitInternal = if (0 < capacity) capacity else -1
    offsetInternal = if (-1 < offset) offset else -1
    return this
  }

  /**
   * Groups the rows using a raw clause.
   *
   * @param clause the raw group by clause
   */
  fun groupBy(clause: String): RawQueryBuilder {
    groupByInternal = clause
    return this
  }

  /**
   * Groups the rows by a specific table and entity.
   *
   * @param table the table specification to group by
   * @param entity the entity name within [table] to group by
   */
  fun groupBy(table: TableSpecification, entity: String): RawQueryBuilder {
    groupByInternal = table.prefix(entity, ::escape)
    return this
  }

  /**
   * Groups the rows by a table's primary key.
   *
   * @param join the table specification along with its primary key
   */
  fun groupBy(join: JoinSpecification): RawQueryBuilder = groupBy(join.table, join.key)

  /**
   * Orders the rows using a raw clause.
   *
   * @param clause the raw order by clause
   * @param ascending whether to order in an ascending (default) or a descending order
   */
  fun orderBy(clause: String, ascending: Boolean = true): RawQueryBuilder {
    orderByInternal = clause
    ascendingInternal = ascending
    return this
  }

  /**
   * Orders the rows by a specific table and entity.
   *
   * @param table the table specification to order by
   * @param entity the entity name within [table] to order by
   * @param ascending whether to order in an ascending (default) or a descending order
   */
  fun orderBy(
    table: TableSpecification,
    entity: String,
    ascending: Boolean = true,
  ): RawQueryBuilder = orderBy(table.prefix(entity, ::escape), ascending)

  /**
   * Orders the rows by a specific table and entity.
   *
   * @param join the table specification along with its primary key
   * @param ascending whether to order in an ascending (default) or a descending order
   */
  fun orderBy(join: JoinSpecification, ascending: Boolean = true): RawQueryBuilder =
    orderBy(join.table, join.key, ascending)

  /**
   * Escapes an entity using the defined [field separator][fieldSeparator].
   *
   * @param entity the entity to escape
   */
  fun escape(entity: String) =
    if ("*" == entity) entity else "$fieldSeparator$entity$fieldSeparator"

  private fun joinInternal(
    @Suppress("SameParameterValue") type: String,
    on: Collection<JoinPair>,
  ) {
    on.forEach { join ->
      joins.add(
        "${type}JOIN ${join.first.table.toString(::escape)} " +
          "ON ${join.first.prefix(::escape)}=${join.second.prefix(::escape)}",
      )
    }
  }

  companion object {
    /**
     * Default initial capacity of internal lists.
     */
    const val INITIAL_CAPACITY: Int = 10

    /**
     * Minimum size of a block of memory.
     */
    const val MINIMUM_BLOCK_SIZE: Int = 512
  }
}
