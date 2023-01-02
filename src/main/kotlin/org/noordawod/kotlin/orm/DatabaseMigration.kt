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

package org.noordawod.kotlin.orm

import com.j256.ormlite.stmt.StatementBuilder
import com.j256.ormlite.support.DatabaseConnection
import org.noordawod.kotlin.orm.migration.Migration
import org.noordawod.kotlin.orm.migration.Migrator
import org.noordawod.kotlin.orm.query.QueryResults
import org.noordawod.kotlin.orm.query.impl.QueryResultsImpl

/**
 * Provides a database migration service to the [Migration] runner.
 */
class DatabaseMigration constructor(
  database: BaseDatabase,
  connection: DatabaseConnection
) {
  private val connection = object : Migrator.Connection {
    override fun execute(statement: String): Int = connection.executeStatement(
      statement,
      DatabaseConnection.DEFAULT_RESULT_FLAGS
    )

    override fun query(statement: String): QueryResults =
      QueryResultsImpl(
        connection.compileStatement(
          statement.trim(),
          StatementBuilder.StatementType.SELECT_RAW,
          null,
          DatabaseConnection.DEFAULT_RESULT_FLAGS,
          false
        )
      )

    override fun queryForLong(statement: String): Long = connection.queryForLong(statement)

    override fun escapeProperty(name: String): String = database.escapeProperty(name)

    override fun escapeValue(value: String): String = database.escapeValue(value)

    override fun escapeLike(value: String): String = database.escapeLike(value)
  }

  /**
   * Executes a [migration] found in the specified [path][basePath].
   */
  fun execute(
    basePath: String,
    migration: Migration
  ) {
    execute(basePath, listOf(migration))
  }

  /**
   * Executes a [migration] found in the specified [paths][basePaths].
   */
  fun execute(
    basePaths: Collection<String>,
    migration: Migration
  ) {
    execute(basePaths, listOf(migration))
  }

  /**
   * Executes a list of [migrations] found in the specified [paths][basePaths].
   */
  fun execute(
    basePaths: Collection<String>,
    migrations: Collection<Migration>
  ) {
    for (basePath in basePaths) {
      val file = java.io.File(basePath)
      if (file.isDirectory && file.canRead()) {
        return execute(basePath, migrations)
      }
    }
    error("No valid database migration path found in: $basePaths")
  }

  /**
   * Executes a list of [migrations] found in the specified [path][basePath].
   */
  fun execute(
    basePath: String,
    migrations: Collection<Migration>
  ) {
    Migrator(
      connection = connection,
      basePath = java.io.File(basePath)
    ).execute(migrations.toTypedArray())
  }
}
