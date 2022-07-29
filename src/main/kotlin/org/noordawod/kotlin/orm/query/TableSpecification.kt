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
