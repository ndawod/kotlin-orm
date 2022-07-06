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

/**
 * Lists fields used in the migrations table.
 */
object MigrationField {
  /**
   * The field "migration_id" in the migrations table.
   */
  const val ID: String = "migration_id"

  /**
   * The field "migration_description" in the migrations table.
   */
  const val DESCRIPTION: String = "migration_description"

  /**
   * The field "migration_file" in the migrations table.
   */
  const val FILE: String = "migration_file"

  /**
   * The field "migration_created" in the migrations table.
   */
  const val CREATED: String = "migration_created"
}

/**
 * A contract for upgrading tables.
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
  fun executePre(connection: Migrator.Connection)

  /**
   * Executes a piece of code after the migration has successfully completed.
   */
  @Throws(java.sql.SQLException::class)
  fun executePost(connection: Migrator.Connection)
}

/**
 * A helper migration class that contains no pre- or post-commands for execution.
 */
abstract class BaseMigration : Migration {
  @Throws(java.sql.SQLException::class)
  override fun executePre(connection: Migrator.Connection) {
    // NO-OP.
  }

  @Throws(java.sql.SQLException::class)
  override fun executePost(connection: Migrator.Connection) {
    // NO-OP.
  }
}
