/*
 * The MIT License
 *
 * Copyright 2023 Noor Dawod. All rights reserved.
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

package org.noordawod.kotlin.orm.migration

import org.noordawod.kotlin.orm.BaseDatabase

/**
 * Provides a database migration runner.
 */
class MigrationRunner(private val database: BaseDatabase) {
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
    database.readWriteConnection { databaseConnection ->
      try {
        Migrator(
          databaseConnection = databaseConnection,
          escapeProperty = database::escapeProperty,
          escapeValue = database::escapeValue,
          escapeLike = database::escapeLike,
          basePath = java.io.File(basePath)
        ).execute(migrations.toTypedArray())
      } finally {
        databaseConnection.closeQuietly()
      }
    }
  }
}
