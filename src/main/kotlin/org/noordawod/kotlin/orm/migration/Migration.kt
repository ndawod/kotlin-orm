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

@file:Suppress("unused", "VariableMinLength")

package org.noordawod.kotlin.orm.migration

import org.noordawod.kotlin.orm.query.QueryResults

/**
 * A database connection interface dedicated for migrations.
 */
interface MigrationConnection {
  /**
   * Executes the specified [statement] and returns how many rows were affected.
   *
   * @param statement the database statement to execute
   */
  @Throws(java.sql.SQLException::class)
  fun execute(statement: String): Int

  /**
   * Queries the database with the specified [statement] and returns the result set.
   *
   * @param statement the database statement for the query
   */
  @Throws(java.sql.SQLException::class)
  fun query(statement: String): QueryResults

  /**
   * Queries the database with the specified [statement] and returns the first result as a [Long].
   *
   * @param statement the database statement for the query
   */
  @Throws(java.sql.SQLException::class)
  fun queryForLong(statement: String): Long

  /**
   * Escapes the provided string property (table name, column name, etc.) and
   * returns the escaped value.
   *
   * @param name the property name
   */
  fun escapeProperty(name: String): String

  /**
   * Escapes the provided string value and returns the escaped value.
   *
   * @param value the value to escape
   */
  fun escapeValue(value: String): String

  /**
   * Escapes the provided string used in a LIKE operation returns the escaped value.
   *
   * @param value the value to escape
   */
  fun escapeLike(value: String): String
}

/**
 * A contract for upgrading a database.
 */
interface Migration {
  /**
   * Returns this migration's version.
   */
  val version: Int

  /**
   * Returns a description of this migration.
   */
  val description: String

  /**
   * Returns the file name that contains the migration commands.
   */
  val file: String

  /**
   * Executes a piece of code before the migration has started.
   */
  @Throws(java.sql.SQLException::class)
  fun executePre(connection: MigrationConnection)

  /**
   * Executes a piece of code after the migration has successfully completed.
   */
  @Throws(java.sql.SQLException::class)
  fun executePost(connection: MigrationConnection)
}

/**
 * A helper migration class that contains no pre- or post-commands for execution.
 */
abstract class BaseMigration : Migration {
  @Throws(java.sql.SQLException::class)
  override fun executePre(connection: MigrationConnection) {
    // NO-OP.
  }

  @Throws(java.sql.SQLException::class)
  override fun executePost(connection: MigrationConnection) {
    // NO-OP.
  }
}
