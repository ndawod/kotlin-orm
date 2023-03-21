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
  "CyclomaticComplexMethod",
  "MagicNumber",
  "TooManyFunctions",
  "StringLiteralDuplication",
  "unused"
)

package org.noordawod.kotlin.orm.migration

import com.diogonunes.jcolor.Ansi.colorize
import com.diogonunes.jcolor.Attribute
import org.noordawod.kotlin.core.Constants
import org.noordawod.kotlin.core.extension.secondsSinceEpoch
import org.noordawod.kotlin.core.extension.trimOrNull
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
          listOf(
            "SELECT COUNT($escapedIdProperty)",
            "FROM $escapedTableName",
            "WHERE $escapedCreatedProperty IS NULL"
          ).joinToString(separator = " ")
        )
      } catch (ignored: java.sql.SQLException) {
        // NO-OP.
      }
      return false
    }

  /**
   * Initialize the migrator before any migration is [executed][execute].
   */
  @Throws(java.sql.SQLException::class)
  fun initialize() {
    ensureMigrationsTable()
  }

  /**
   * Performs the specified migration steps.
   *
   * @param migrations list of migration to perform
   */
  @Suppress("LongMethod", "NestedBlockDepth")
  @Throws(java.sql.SQLException::class, java.io.IOException::class)
  fun execute(migrations: Array<Migration>) {
    // Reusable variables.
    var isFirstRun = true

    // Get current version
    var nextVersion: Int = version()
    println("- Current version: " + colorize("$nextVersion", BOLD_TEXT))
    println("- Number of migrations: " + colorize("${migrations.size}", BOLD_TEXT))

    // Keep track of timing for all migrations.
    val migrationsStart = java.util.Date()

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

      if (isFirstRun) {
        isFirstRun = false
        println("- Running migrations:")
      }

      var preMigrationRan = false
      var postMigrationRan = false
      var migrationRan = false
      var migrationError: Throwable? = null

      // Keep track of timing for this migration.
      val migrationStart = java.util.Date()

      // Each migration runs in its own transaction so in case it fails, we can roll back.
      connection.execute("START TRANSACTION")

      try {
        println(
          "  - " +
            colorize("v$nextVersion", BRIGHT_GREEN_TEXT) +
            " → " +
            colorize(migration.description, BOLD_TEXT) +
            ":"
        )

        lockMigration(migration)

        performPreMigration(migration)
        preMigrationRan = true

        executedCommands.clear()
        performMigration(
          migration = migration,
          nextVersion = nextVersion,
          executedCommands = executedCommands
        )
        migrationRan = true

        performPostMigration(migration)
        postMigrationRan = true

        unlockMigration(migration)
      } catch (error: java.sql.SQLException) {
        migrationError = error
        println(colorize(" SQL ERROR!", BRIGHT_RED_TEXT))
      } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
        migrationError = error
        println(colorize(" GENERIC ERROR!", BRIGHT_RED_TEXT))
      } finally {
        if (null == migrationError) {
          performCommit()
        } else {
          performRollback()
        }

        val migrationEnd = java.util.Date()
        println("  - Started: " + colorize("$migrationStart", BOLD_TEXT))
        println("  - Ended: " + colorize("$migrationEnd", BOLD_TEXT))
        val duration = migrationEnd.time - migrationStart.time
        if (1 < duration) {
          println("  - Duration: " + colorize("$duration milliseconds", BOLD_TEXT))
        }
        println()
      }

      if (null != migrationError) {
        when {
          !preMigrationRan ->
            println(
              colorize(
                "Unexpected error while running pre-migration code.",
                BRIGHT_RED_TEXT
              )
            )

          !migrationRan -> {
            println(
              colorize(
                "Unexpected error while running migration.",
                BRIGHT_RED_TEXT
              )
            )

            // Report all executed commands in this migration.
            if (executedCommands.isNotEmpty()) {
              println()
              println(
                colorize(
                  "These migration commands were already executed – " +
                    "the last one probably caused the error:",
                  BRIGHT_RED_TEXT
                )
              )
              println()
              for (command in executedCommands) {
                println("> $command")
              }
            }
          }

          !postMigrationRan ->
            println(
              colorize(
                "Unexpected error while running post-migration code.",
                BRIGHT_RED_TEXT
              )
            )

          else ->
            println(
              colorize(
                "Unexpected error while unlocking migrations table.",
                BRIGHT_RED_TEXT
              )
            )
        }

        println()

        throw migrationError
      }
    }

    executedCommands.clear()

    val migrationsEnd = java.util.Date()
    val duration = migrationsEnd.time - migrationsStart.time

    println("- Started: " + colorize("$migrationsStart", BOLD_TEXT))
    println("- Ended: " + colorize("$migrationsEnd", BOLD_TEXT))
    println()
    println(
      colorize(
        "Database migration finished in $duration milliseconds.",
        BOLD_TEXT
      )
    )
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

  private fun performCommit() {
    performCommitOrRollback("COMMIT")
  }

  private fun performRollback() {
    performCommitOrRollback("ROLLBACK")
  }

  private fun performCommitOrRollback(command: String) {
    try {
      print("    - Executing ${colorize(command, BOLD_TEXT)}…")
      connection.execute(command)
      println(" Done.")
    } catch (ignored: java.sql.SQLException) {
      println()
      println(
        colorize(
          "Ignored an exception issuing a $command operation.",
          BOLD_TEXT
        )
      )
    }
  }

  @Throws(java.sql.SQLException::class)
  private fun performPreMigration(migration: Migration) {
    print("    - Running pre-migration code…")
    migration.executePre(connection)
    println(" Done.")
  }

  @Throws(java.sql.SQLException::class)
  private fun performPostMigration(migration: Migration) {
    print("    - Running post-migration code…")
    migration.executePost(connection)
    println(" Done.")
  }

  @Throws(java.sql.SQLException::class)
  private fun performMigration(
    migration: Migration,
    nextVersion: Int,
    executedCommands: QueryCommands
  ) {
    print("    - Running migration:")

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
        print(colorize(" $percent%", BOLD_TEXT))
      }
    }

    // Last debugging.
    if (100 != percent) {
      print(colorize(" 100%", BOLD_TEXT))
    }

    println(".")
  }

  @Throws(java.sql.SQLException::class)
  private fun lockMigration(migration: Migration) {
    print("    - Locking ${colorize(escapedTableName, BOLD_TEXT)} table…")

    val fieldValues = mapOf(
      escapedIdProperty to "${migration.version}",
      escapedDescriptionProperty to connection.escapeValue(migration.description),
      escapedFileProperty to connection.escapeValue(migration.file)
    )

    connection.execute(
      listOf(
        "INSERT INTO $escapedTableName (",
        fieldValues.keys.joinToString(separator = ","),
        ") VALUES (",
        fieldValues.values.joinToString(separator = ","),
        ")"
      ).joinToString(separator = "")
    )

    println(" Done.")
  }

  @Throws(java.sql.SQLException::class)
  private fun unlockMigration(migration: Migration) {
    print("    - Unlocking ${colorize(escapedTableName, BOLD_TEXT)} table… ")

    connection.execute(
      listOf(
        "UPDATE $escapedTableName",
        "SET $escapedCreatedProperty=${java.util.Date().secondsSinceEpoch()}",
        "WHERE $escapedIdProperty=${migration.version}"
      ).joinToString(separator = " ")
    )

    println(" Done.")
  }

  @Throws(java.io.IOException::class)
  private fun readFile(upgradeFile: java.io.File): Collection<String>? =
    java.nio.file.Files
      .readAllLines(java.nio.file.Paths.get(upgradeFile.toURI()))
      .ifEmpty { null }

  @Suppress("LoopWithTooManyJumpStatements")
  private fun parseCommands(commands: Collection<String>): Collection<String> =
    ArrayList<String>(Constants.MEDIUM_BLOCK_SIZE).apply {
      val nextCommand = StringBuilder(Constants.MEDIUM_BLOCK_SIZE)

      for (command in commands) {
        val normalizedCommand = command.trimOrNull() ?: continue
        var isComment = false

        // Detect if this line is a comment.
        // Note: We do not support multiple-line comments.
        val commentPrefixesIterator = commentPrefixes.iterator()
        while (!isComment && commentPrefixesIterator.hasNext()) {
          isComment = normalizedCommand.startsWith(commentPrefixesIterator.next())
        }

        if (isComment) {
          continue
        }

        if (normalizedCommand.endsWith(';')) {
          // The command ends with a semicolon; it's the final piece of the command.
          nextCommand.append(" ${normalizedCommand.substring(0, normalizedCommand.length - 1)}")
          add("$nextCommand")
          nextCommand.clear()
        } else {
          nextCommand.append(" $normalizedCommand")
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
        listOf(
          "CREATE TABLE $escapedTableName (",
          listOf(
            "$escapedIdProperty smallint unsigned NOT NULL",
            "$escapedDescriptionProperty tinytext $asciiCollation NOT NULL",
            "$escapedFileProperty tinytext $asciiCollation NOT NULL",
            "$escapedCreatedProperty int unsigned NULL",
            "PRIMARY KEY ($escapedIdProperty)",
            "KEY $escapedCreatedProperty ($escapedCreatedProperty)"
          ).joinToString(separator = ","),
          ")"
        ).joinToString(separator = "")
      )
      println("- Migrations table missing, auto-created.")
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
    const val TABLE_NAME = "\$migration"

    /**
     * Default list of prefixes used for commenting migration plans.
     */
    val DEFAULT_COMMENT_PREFIXES: Collection<String> = listOf("/*", "#", "-- ")

    private val BOLD_TEXT = Attribute.BOLD()
    private val BRIGHT_GREEN_TEXT = Attribute.BRIGHT_GREEN_TEXT()
    private val BRIGHT_RED_TEXT = Attribute.BRIGHT_RED_TEXT()
  }
}
