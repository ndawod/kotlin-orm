/*
 * The MIT License
 *
 * Copyright 2021 Noor Dawod. All rights reserved.
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

package org.noordawod.kotlin.orm

/**
 * A signature of a [Pair] of [JoinSpecification]s.
 */
typealias JoinPair = Pair<JoinSpecification, JoinSpecification>

/**
 * A signature of a function that accepts a String and returns a String.
 */
typealias EscapeCallback = (String) -> String

/**
 * A data class holding a table name and its alias, if specified.
 *
 * @param name the table name
 * @param alias the table alias, optional
 */
data class TableSpecification constructor(
  val name: String,
  val alias: String?
) {
  override fun toString(): String = if (null == alias) name else "$name AS $alias"

  /**
   * Returns a string representation of this table and its alias suitable to be used in a
   * SQL statement.
   *
   * @param escape handler to escape table and alias names
   */
  inline fun toString(escape: EscapeCallback): String {
    val nameEscaped = escape(name)
    return if (null == alias) nameEscaped else "$nameEscaped AS ${escape(alias)}"
  }

  /**
   * Returns a prefix for this table suitable to be used in a SQL statement.
   *
   * @param escape handler to escape table and alias names
   */
  fun prefix(escape: EscapeCallback): String = if (null == alias) "" else "${escape(alias)}."

  /**
   * Returns the specified entity with a prefix of this table suitable to be used in a
   * SQL statement.
   *
   * @param entity entity to prefix
   * @param escape handler to escape table and alias names
   */
  fun prefix(entity: String, escape: EscapeCallback): String = "${prefix(escape)}${escape(entity)}"
}

/**
 * A data class describing one part of a JOIN operation.
 *
 * @param table the table specification
 * @param key the primary key in [table]
 */
data class JoinSpecification constructor(
  val table: TableSpecification,
  val key: String
) {
  fun prefix(escape: EscapeCallback): String = table.prefix(key, escape)
}

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
 * @param fieldSeparator the character is used to escape table and entity names
 * @param initialCapacity initial capacity of internal lists
 */
@Suppress("TooManyFunctions")
class RawQueryBuilder constructor(val fieldSeparator: Char, initialCapacity: Int) {
  /**
   * Returns a new [RawQueryBuilder] instance with a default initial capacity.
   *
   * @param fieldSeparator the character is used to escape table and entity names
   */
  constructor(fieldSeparator: Char) : this(fieldSeparator, INITIAL_CAPACITY)

  private val tables = LinkedHashSet<TableSpecification>(initialCapacity)
  private val entities = LinkedHashSet<String>(initialCapacity)
  private val joins = LinkedHashSet<String>(initialCapacity)
  private val wheres = LinkedHashSet<String>(initialCapacity)

  private var limitInternal: Int = -1
  private var offsetInternal: Int = -1
  private var groupByInternal: String? = null
  private var orderByInternal: String? = null
  private var ascendingInternal: Boolean = true

  @Suppress("NestedBlockDepth", "ComplexFunction")
  override fun toString(): String = StringBuilder(MINIMUM_BLOCK_SIZE).apply {
    append("SELECT ")
    append(entities.joinToString())
    append(" FROM ")
    tables.forEach {
      append(it.toString(::escape))
    }
    joins.forEach {
      append(" $it")
    }
    if (wheres.isNotEmpty()) {
      append(" WHERE (")
      var firstClause = true
      wheres.forEach {
        if (firstClause) {
          firstClause = false
        } else {
          append(" AND ")
        }
        append(it)
      }
      append(")")
    }
    if (!groupByInternal.isNullOrEmpty()) {
      append(" GROUP BY $groupByInternal")
    }
    if (!orderByInternal.isNullOrEmpty()) {
      append(" ORDER BY $orderByInternal ${if (ascendingInternal) "ASC" else "DESC"}")
    }
    if (0 < limitInternal) {
      append(" LIMIT $limitInternal")
      if (0 < offsetInternal) {
        append(" OFFSET $offsetInternal")
      }
    }
  }.toString()

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
   * Adds a new clause to the WHERE part.
   *
   * @param clause clause to add
   */
  fun where(clause: String): RawQueryBuilder {
    wheres.add(clause)
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
    ascending: Boolean = true
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
    on: Collection<JoinPair>
  ) {
    on.forEach { join ->
      joins.add(
        "${type}JOIN ${join.first.table.toString(::escape)} " +
          "ON ${join.first.prefix(::escape)}=${join.second.prefix(::escape)}"
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
