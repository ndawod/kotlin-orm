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

@file:Suppress(
  "unused",
  "MagicNumber",
  "TooManyFunctions",
  "CyclomaticComplexMethod"
)

package org.noordawod.kotlin.orm.migration

import org.noordawod.kotlin.core.Constants
import org.noordawod.kotlin.core.extension.secondsSinceEpoch
import org.noordawod.kotlin.orm.query.QueryCommands
import org.noordawod.kotlin.orm.query.QueryResults

/**
 * This class can migrate the database schema to its most recent defined version (above).
 *
 * @param connection the Connection implementation to use for querying the database
 * @param tableName the table name to store meta-data about migrations
 * @param basePath base path to where the Migrator will find migration plans
 * @param commentPrefixes list of prefixes used for commenting migration plans
 */
class Migrator constructor(
  private val connection: Connection,
  private val tableName: String = TABLE_NAME,
  private val basePath: java.io.File,
  private val commentPrefixes: Collection<String> = DEFAULT_COMMENT_PREFIXES
) {
  private val escapedTableName = connection.escapeProperty(tableName)
  private val escapedIdProperty = connection.escapeProperty(MigrationField.ID)
  private val escapedDescriptionProperty = connection.escapeProperty(MigrationField.DESCRIPTION)
  private val escapedFileProperty = connection.escapeProperty(MigrationField.FILE)
  private val escapedCreatedProperty = connection.escapeProperty(MigrationField.CREATED)
  private val asciiCollation = "CHARACTER SET ascii COLLATE ascii_general_ci"

  private val isLocked: Boolean
    get() {
      try {
        return 0L != connection.queryForLong(
          "SELECT COUNT($escapedIdProperty) AS count " +
            "FROM $escapedTableName " +
            "WHERE $escapedCreatedProperty IS NULL"
        )
      } catch (ignored: java.sql.SQLException) {
        // NO-OP.
      }
      return false
    }

  /**
   * Performs the specified migration steps.
   *
   * @param migrations list of migration to perform
   */
  @Suppress("LongMethod", "NestedBlockDepth")
  @Throws(java.sql.SQLException::class, java.io.IOException::class)
  fun execute(migrations: Array<Migration>) {
    // Make sure migrations' table exists.
    ensureMigrationsTable()

    // Reusable variables.
    var isFirstRun = true

    // Get current version
    var nextVersion: Int = version()
    println("- Current version: $nextVersion")

    // Keep track of timing for all migrations.
    val migrationsStart = java.util.Date()
    println("- Started: $migrationsStart")

    // Keep track of executed commands in this migration.
    val executedCommands = QueryCommands(Constants.MEDIUM_BLOCK_SIZE)

    // Go over all migrations and run them one after the other.
    for (migration in migrations) {
      // Get the next migration plan and check if it's already executed.
      if (migration.version <= nextVersion) {
        continue
      }

      // Check if the migrations' table is locked.
      if (isLocked) {
        throw java.sql.SQLException("Migration table is locked, is another process active?")
      }

      // Migration plans must be continuous.
      nextVersion++
      if (migration.version != nextVersion) {
        // Either this is a migration we did before, or out of sync.
        throw java.sql.SQLException(
          "Migration plan #$nextVersion is not continuous," +
            " database migration is out of sync!"
        )
      }

      // Debug.
      if (isFirstRun) {
        isFirstRun = false
        println("- Running migrations:")
      }

      var preMigrationRan = false
      var migrationRan = false
      var migrationError: java.sql.SQLException? = null

      // Keep track of timing for this migration.
      val migrationStart = java.util.Date()

      try {
        // Each migration runs in its own transaction so in case it fails, we can roll back.
        connection.execute("SET autocommit=0")
        connection.execute("START TRANSACTION")

        lockMigration(migration)

        print("  v$nextVersion: ${migration.description}:")

        performPreMigration(migration)
        preMigrationRan = true

        performMigration(migration, nextVersion, executedCommands)
        migrationRan = true

        performPostMigration(migration)

        performCommitOrRollback("COMMIT")

        unlockMigration(migration)
      } catch (error: java.sql.SQLException) {
        migrationError = error
        // An SQL error has occurred, we need to roll back all changes...
        performCommitOrRollback("ROLLBACK")
      } finally {
        executedCommands.clear()
        val migrationEnd = java.util.Date()
        println("  - Started: $migrationStart")
        println("  - Ended: $migrationEnd")
        val duration = migrationEnd.time - migrationStart.time
        if (1 < duration) {
          println("  - Duration: $duration milliseconds")
        }
      }

      if (null != migrationError) {
        println()

        when {
          !preMigrationRan -> println("Unexpected error while running pre-migration hook.")

          !migrationRan -> {
            println("Unexpected error while running migration.")

            // Report all executed commands in this migration.
            if (executedCommands.isNotEmpty()) {
              println()
              println(
                "These migration commands were already executed – " +
                  "the last one probably caused the error:"
              )
              println()
              for (command in executedCommands) {
                println("> $command")
              }
            }
          }

          else -> println("Unexpected error while running post-migration hook.")
        }

        println()

        throw migrationError
      }
    }

    val migrationsEnd = java.util.Date()
    val duration = migrationsEnd.time - migrationsStart.time

    println("- Ended: $migrationsEnd")
    println()
    println("Database Migration finished in $duration milliseconds.")
    println()
  }

  /**
   * Returns the latest version of the database.
   */
  @Suppress("MemberVisibilityCanBePrivate")
  fun version(): Int {
    try {
      return connection.queryForLong(
        "SELECT MAX($escapedIdProperty) AS max_version " +
          "FROM $escapedTableName"
      ).toInt()
    } catch (ignored: java.sql.SQLException) {
      // NO-OP.
    }
    return 0
  }

  private fun performCommitOrRollback(command: String) {
    try {
      connection.execute(command)
    } catch (ignored: java.sql.SQLException) {
      println()
      println("Unhandled exception issuing a $command to finalize the migration plan.")
    }
  }

  @Throws(java.sql.SQLException::class)
  private fun performPreMigration(migration: Migration) {
    println("    - Running pre-migration code…")
    migration.executePre(connection)
  }

  @Throws(java.sql.SQLException::class)
  private fun performPostMigration(migration: Migration) {
    println("    - Running post-migration code…")
    migration.executePost(connection)
  }

  @Throws(java.sql.SQLException::class)
  private fun performMigration(
    migration: Migration,
    nextVersion: Int,
    executedCommands: QueryCommands
  ) {
    // Debugging.
    print("    - Running migration:")

    try {
      // Where the upgrade file resides.
      val upgradeFile = java.io.File(basePath, migration.file)

      // Read all commands in the upgrade file.
      val upgradeCommands = readFile(upgradeFile)
        ?: throw java.sql.SQLException("Migration plan #$nextVersion is empty! ($upgradeFile)")

      // Parse the upgrade commands.
      val commands = parseCommands(upgradeCommands)
      val commandsSizePercentage = commands.size * 100f

      // Run the upgrade commands.
      var progress = 0f
      var percent = 0
      for (command in commands) {
        executedCommands.add(command)
        connection.execute(command)
        progress++
        val nextPercent = (progress / commandsSizePercentage).toInt()
        if (10 <= nextPercent - percent) {
          percent += 10
          print(" $percent%")
        }
      }

      // Last debugging.
      if (100 != percent) {
        print(" 100%")
      }

      print(".")
    } catch (error: java.sql.SQLException) {
      println()
      throw error
    } finally {
      println()
    }
  }

  @Throws(java.sql.SQLException::class)
  private fun lockMigration(migration: Migration) {
    connection.execute(
      arrayOf(
        "INSERT INTO $escapedTableName (",
        "$escapedIdProperty,",
        "$escapedDescriptionProperty,",
        "$escapedFileProperty,",
        ") VALUES (",
        "${migration.version},",
        "${connection.escapeValue(migration.description)},",
        "${connection.escapeValue(migration.file)})"
      ).joinToString(separator = "")
    )
  }

  @Throws(java.sql.SQLException::class)
  private fun unlockMigration(migration: Migration) {
    connection.execute(
      "UPDATE $escapedTableName SET " +
        "$escapedCreatedProperty=" +
        "${java.util.Date().secondsSinceEpoch()} WHERE " +
        "$escapedIdProperty=${migration.version}"
    )
  }

  @Throws(java.io.IOException::class)
  private fun readFile(upgradeFile: java.io.File): Collection<String>? =
    java.nio.file.Files
      .readAllLines(java.nio.file.Paths.get(upgradeFile.toURI()))
      .ifEmpty { null }

  @Suppress("LoopWithTooManyJumpStatements")
  private fun parseCommands(commands: Collection<String>): Collection<String> =
    ArrayList<String>(1024).apply {
      var nextCommand = ""

      for (command in commands) {
        val normalizedCommand = command.trim()
        if (normalizedCommand.isEmpty()) {
          continue
        }

        // Detect if this line is a comment. Note: We do not support multiple-line comments.
        val commentPrefixesIterator = commentPrefixes.iterator()
        var isComment = false
        while (!isComment && commentPrefixesIterator.hasNext()) {
          isComment = normalizedCommand.startsWith(commentPrefixesIterator.next())
        }

        if (isComment) {
          continue
        }

        if (normalizedCommand.endsWith(";")) {
          // The command ends with a semicolon; it's the final piece the command.
          nextCommand += " ${normalizedCommand.substring(0, normalizedCommand.length - 1)}"
          add(nextCommand.trim())
          nextCommand = ""
        } else {
          nextCommand += " $normalizedCommand"
        }
      }
    }

  /**
   * Creates the migrations' table if it doesn't exist.
   */
  @Throws(java.sql.SQLException::class)
  private fun ensureMigrationsTable() {
    try {
      connection.execute(
        "CREATE TABLE $escapedTableName (" +
          "$escapedIdProperty smallint UNSIGNED NOT NULL," +
          "$escapedDescriptionProperty tinytext $asciiCollation NOT NULL," +
          "$escapedFileProperty tinytext $asciiCollation NOT NULL," +
          "$escapedCreatedProperty int UNSIGNED NULL," +
          "PRIMARY KEY ($escapedIdProperty)," +
          "KEY $escapedCreatedProperty ($escapedCreatedProperty)" +
          ")"
      )
      println("- Migrations table missing, auto-created...")
    } catch (ignored: java.sql.SQLException) {
      println("- Migrations table exists: $tableName (v${version()})")
    }
  }

  /**
   * Generic database connection interface.
   */
  interface Connection {
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

  companion object {
    /**
     * Default name of the migrations table.
     */
    const val TABLE_NAME = "\$migrations"

    /**
     * Default list of prefixes used for commenting migration plans.
     */
    val DEFAULT_COMMENT_PREFIXES: Collection<String> = listOf("/*", "#", "-- ")
  }
}
