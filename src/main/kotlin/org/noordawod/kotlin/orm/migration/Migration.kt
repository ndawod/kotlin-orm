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
 * The base interface to describe a database migration.
 */
sealed interface Migration {
  /**
   * A description of this migration.
   */
  val description: String

  /**
   * The file name that contains the migration commands.
   */
  val file: String

  /**
   * Executes a piece of code before the migration starts.
   */
  @Throws(java.sql.SQLException::class)
  fun executePre(connection: MigrationConnection)

  /**
   * Executes a piece of code after the migration ends.
   */
  @Throws(java.sql.SQLException::class)
  fun executePost(connection: MigrationConnection)

  /**
   * A migration that doesn't require a version check to run.
   *
   * This kind of migration will execute always whenever the [migrator][MigrationRunner]
   * is launched.
   */
  interface Dump : Migration {
    /**
     * Returns whether this migration should execute.
     *
     * By default, this method returns `true`.
     */
    @Throws(java.sql.SQLException::class)
    fun isExecutable(connection: MigrationConnection): Boolean = true
  }

  /**
   * A migration that runs if its version is newer than the version of the database.
   *
   * This kind of migration will execute only if its [version] is found be to be newer
   * than the version of the associated database.
   */
  interface Versioned : Migration {
    /**
     * Returns this migration's version.
     */
    val version: Int
  }
}

/**
 * A helper [Migration.Dump] class that contains no pre- or post-commands for execution.
 */
abstract class BaseDumpMigration : Migration.Dump {
  @Throws(java.sql.SQLException::class)
  override fun executePre(connection: MigrationConnection) {
    // NO-OP.
  }

  @Throws(java.sql.SQLException::class)
  override fun executePost(connection: MigrationConnection) {
    // NO-OP.
  }
}

/**
 * A helper [Migration.Versioned] class that contains no pre- or post-commands for execution.
 */
abstract class BaseVersionedMigration : Migration.Versioned {
  @Throws(java.sql.SQLException::class)
  override fun executePre(connection: MigrationConnection) {
    // NO-OP.
  }

  @Throws(java.sql.SQLException::class)
  override fun executePost(connection: MigrationConnection) {
    // NO-OP.
  }
}
