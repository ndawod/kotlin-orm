/*
 * The MIT License
 *
 * Copyright 2026 Noor Dawod. All rights reserved.
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

package org.noordawod.kotlin.orm.extension

import org.noordawod.kotlin.core.extension.commaSeparated
import org.noordawod.kotlin.core.extension.joined
import org.noordawod.kotlin.core.extension.mutableListWith
import org.noordawod.kotlin.core.extension.spaceSeparated
import org.noordawod.kotlin.orm.FieldValues
import org.noordawod.kotlin.orm.migration.MigrationConnection

/**
 * Inserts a bunch of rows into a database table.
 *
 * @param table the unescaped table name to insert rows into
 * @param ignoreIfExists do nothing if the same row already exists in the table
 * @param values one or more fields, with unescaped keys and escaped values,
 * to insert into the table
 */
@Throws(java.sql.SQLException::class)
fun MigrationConnection.insertInto(
  table: String,
  ignoreIfExists: Boolean = false,
  values: FieldValues,
) {
  val shouldIgnore = if (ignoreIfExists) " IGNORE" else ""
  val query = listOf(
    "INSERT$shouldIgnore INTO ${table.escapeProperty(this)} (",
    values.keys.escapeProperties(this),
    ") VALUES (",
    values.values.commaSeparated(),
    ")",
  ).joined()

  execute(query)
}

/**
 * Updates a row in a database table.
 *
 * @param table the unescaped table name to update rows in
 * @param set one or more fields, with unescaped keys and escaped values,
 * to update in the table
 * @param where the escaped where clause to use
 */
@Throws(java.sql.SQLException::class)
fun MigrationConnection.update(
  table: String,
  set: FieldValues,
  where: Iterable<String>,
) {
  val setFieldsQuery = mutableListWith<String>(set.size)

  for (entry in set) {
    setFieldsQuery.add("${entry.key.escapeProperty(this)}=${entry.value}")
  }

  val query = listOf(
    "UPDATE ${table.escapeProperty(this)} SET",
    setFieldsQuery.commaSeparated(),
    "WHERE (${where.spaceSeparated()})",
  ).spaceSeparated()

  execute(query)
}

/**
 * Delete one or more rows from a database table.
 *
 * @param table the unescaped table name to delete rows from
 * @param where the escaped where clause to use
 */
@Throws(java.sql.SQLException::class)
fun MigrationConnection.deleteFrom(
  table: String,
  where: Iterable<String>,
) {
  deleteFrom(
    table = table,
    where = where.spaceSeparated(),
  )
}

/**
 * Delete one or more rows from a database table.
 *
 * @param table the unescaped table name to delete rows from
 * @param where the escaped where clause to use
 */
@Throws(java.sql.SQLException::class)
fun MigrationConnection.deleteFrom(
  table: String,
  where: String,
) {
  val query = listOf(
    "DELETE FROM",
    table.escapeProperty(this),
    "WHERE ($where)",
  ).spaceSeparated()

  execute(query)
}
