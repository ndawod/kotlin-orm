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
  "LongParameterList",
  "unused",
)

package org.noordawod.kotlin.orm.migration

import com.diogonunes.jcolor.Ansi.colorize
import com.diogonunes.jcolor.Attribute
import com.j256.ormlite.stmt.StatementBuilder
import com.j256.ormlite.support.DatabaseConnection
import org.noordawod.kotlin.core.Constants
import org.noordawod.kotlin.core.extension.secondsSinceEpoch
import org.noordawod.kotlin.core.extension.trimOrNull
import org.noordawod.kotlin.orm.query.QueryCommands
import org.noordawod.kotlin.orm.query.QueryResults
import org.noordawod.kotlin.orm.query.impl.QueryResultsImpl

/**
 * This class can migrate the database schema to its most recent defined version (above).
 *
 * @param databaseConnection the physical database connection
 * @param escapeProperty the function to escape a property (field, table, etc.)
 * @param escapeValue the function to escape a value
 * @param escapeLike the function to escape a LIKE value
 * @param tableName the table name to store meta-data about migrations
 * @param basePath base path to where the Migrator will find migration plans
 * @param commentPrefixes list of prefixes used for commenting migration plans
 */
internal class Migrator(
  private val databaseConnection: DatabaseConnection,
  escapeProperty: (String) -> String,
  escapeValue: (String) -> String,
  escapeLike: (String, Char?) -> String,
  private val tableName: String = TABLE_NAME,
  private val basePath: java.io.File,
  private val commentPrefixes: Collection<String> = DEFAULT_COMMENT_PREFIXES,
) {
  private val connection = object : MigrationConnection {
    override fun execute(statement: String): Int = databaseConnection.executeStatement(
      statement,
      DatabaseConnection.DEFAULT_RESULT_FLAGS,
    )

    override fun query(statement: String): QueryResults =
      QueryResultsImpl(
        databaseConnection.compileStatement(
          statement.trim(),
          StatementBuilder.StatementType.SELECT_RAW,
          null,
          DatabaseConnection.DEFAULT_RESULT_FLAGS,
          false,
        ),
      )

    override fun queryForLong(statement: String): Long = databaseConnection.queryForLong(statement)

    override fun escapeProperty(name: String): String = escapeProperty(name)

    override fun escapeValue(value: String): String = escapeValue(value)

    override fun escapeLike(value: String): String = escapeLike(value, null)
  }

  private val escapedTableName = escapeProperty(tableName)
  private val escapedIdProperty = escapeProperty(ID)
  private val escapedDescriptionProperty = escapeProperty(DESCRIPTION)
  private val escapedFileProperty = escapeProperty(FILE)
  private val escapedCreatedProperty = escapeProperty(CREATED)
  private val asciiCollation = "CHARACTER SET ascii COLLATE ascii_general_ci"
  private val executedCommands = QueryCommands(Constants.MEDIUM_BLOCK_SIZE)

  private val isLocked: Boolean
    get() = try {
      0L != connection.queryForLong(
        listOf(
          "SELECT COUNT($escapedIdProperty)",
          "FROM $escapedTableName",
          "WHERE $escapedCreatedProperty IS NULL",
        ).joinToString(separator = " "),
      )
    } catch (ignored: java.sql.SQLException) {
      false
    }

  private var isMigrationTableInitialized = false

  /**
   * Returns the latest version of the database.
   */
  @Suppress("MemberVisibilityCanBePrivate")
  fun version(): Int {
    try {
      return connection.queryForLong(
        "SELECT MAX($escapedIdProperty) AS max_version " +
          "FROM $escapedTableName",
      ).toInt()
    } catch (ignored: java.sql.SQLException) {
      // NO-OP.
    }
    return 0
  }

  /**
   * Performs the specified [dump migrations][Migration.Dump].
   *
   * @param migrations list of migration to perform
   */
  @Suppress("LongMethod", "NestedBlockDepth")
  @Throws(java.sql.SQLException::class, java.io.IOException::class)
  fun executeDump(migrations: Collection<Migration.Dump>) {
    initMigrations()

    var isFirstRun = true
    var index = 0
    val migrationsCount = migrations.size

    println("- Current version: " + colorize("${version()}", BOLD_TEXT))
    println("- Number of dump migrations: " + colorize("$migrationsCount", BOLD_TEXT))

    // Keep track of timing for all migrations.
    val migrationsStart = java.util.Date()

    // Go over all migrations and run them one after the other.
    for (migration in migrations) {
      index++

      if (isFirstRun) {
        isFirstRun = false
        println("- Running:")
      }

      print(
        "  - " +
          colorize("$index of $migrationsCount", BRIGHT_GREEN_TEXT) +
          " → " +
          colorize(migration.description, BOLD_TEXT) +
          ":",
      )

      // These migration can decide for themselves whether to execute or not.
      if (!migration.isExecutable(connection)) {
        println(" Skipped…")
        continue
      }

      println()

      val migrationStart = java.util.Date()
      val migrationState = MigrationState()

      // Each migration runs in its own transaction so in case it fails, we can roll back.
      val savePoint = databaseConnection.setSavePoint("DUMP_MIGRATION_$index")

      executedCommands.clear()

      try {
        migration.performPre()
        migrationState.preRan = true

        migration.perform()
        migrationState.ran = true

        migration.performPost()
        migrationState.postRan = true
      } catch (error: java.sql.SQLException) {
        error.capture(
          migrationState = migrationState,
          message = " SQL ERROR!",
        )
      } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
        error.capture(
          migrationState = migrationState,
          message = " GENERIC ERROR!",
        )
      } finally {
        savePoint?.handleSavePoint(migrationState)

        migrationStart.printFooter()
      }

      migrationState.possiblyHandleError()
    }

    migrationsStart.printSummary()
  }

  /**
   * Performs the specified [versioned migrations][Migration.Versioned].
   *
   * @param migrations list of migration to perform
   */
  @Suppress("LongMethod", "NestedBlockDepth")
  @Throws(java.sql.SQLException::class, java.io.IOException::class)
  fun executeVersioned(migrations: Collection<Migration.Versioned>) {
    initMigrations()

    var isFirstRun = true
    val migrationsCount = migrations.size
    var nextVersion: Int = version()

    println("- Current version: " + colorize("$nextVersion", BOLD_TEXT))
    println("- Number of versioned migrations: " + colorize("$migrationsCount", BOLD_TEXT))

    // Keep track of timing for all migrations.
    val migrationsStart = java.util.Date()

    // Go over all migrations and run them one after the other.
    for (migration in migrations) {
      // Check if the migrations' table is locked.
      if (isLocked) {
        throw java.sql.SQLException("Migration table is locked, is another process active?")
      }

      // We'll execute new migration plans only based on their version.
      if (migration.version <= nextVersion) {
        continue
      }

      // Migration plans must be continuous.
      nextVersion++
      if (migration.version != nextVersion) {
        // Either this is a migration we did before, or out of sync.
        throw java.sql.SQLException(
          "Migration plan #$nextVersion is not continuous," +
            " database migration is out of sync!",
        )
      }

      if (isFirstRun) {
        isFirstRun = false
        println("- Running migrations:")
      }

      val migrationStart = java.util.Date()
      val migrationState = MigrationState()

      // Each migration runs in its own transaction so in case it fails, we can roll back.
      val savePoint = databaseConnection.setSavePoint("MIGRATION_V$nextVersion")

      executedCommands.clear()

      try {
        println(
          "  - " +
            colorize("v$nextVersion", BRIGHT_GREEN_TEXT) +
            " → " +
            colorize(migration.description, BOLD_TEXT) +
            ":",
        )

        migration.lock()

        migration.performPre()
        migrationState.preRan = true

        migration.perform()
        migrationState.ran = true

        migration.performPost()
        migrationState.postRan = true

        migration.unlock()
      } catch (error: java.sql.SQLException) {
        error.capture(
          migrationState = migrationState,
          message = " SQL ERROR!",
        )
      } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
        error.capture(
          migrationState = migrationState,
          message = " GENERIC ERROR!",
        )
      } finally {
        savePoint?.handleSavePoint(migrationState)

        migrationStart.printFooter()
      }

      migrationState.possiblyHandleError()
    }

    migrationsStart.printSummary()
  }

  @Throws(java.sql.SQLException::class)
  private fun initMigrations() {
    databaseConnection.isAutoCommit = false

    if (isMigrationTableInitialized) {
      return
    }

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
            "KEY $escapedCreatedProperty ($escapedCreatedProperty)",
          ).joinToString(separator = ","),
          ")",
        ).joinToString(separator = ""),
      )
      println("- Migrations table missing, auto-created.")
    } catch (ignored: java.sql.SQLException) {
      println("- Migrations table exists: $tableName")
    } finally {
      isMigrationTableInitialized = true
    }
  }

  private fun java.util.Date.printFooter() {
    val migrationEnd = java.util.Date()
    println("  - Started: " + colorize("$this", BOLD_TEXT))
    println("  - Ended: " + colorize("$migrationEnd", BOLD_TEXT))
    val duration = migrationEnd.time - time
    if (1 < duration) {
      println("  - Duration: " + colorize("$duration milliseconds", BOLD_TEXT))
    }
    println()
  }

  private fun java.util.Date.printSummary() {
    executedCommands.clear()

    val migrationsEnd = java.util.Date()
    val duration = migrationsEnd.time - time

    println("- Started: " + colorize("$this", BOLD_TEXT))
    println("- Ended: " + colorize("$migrationsEnd", BOLD_TEXT))
    println()
    println(
      colorize(
        "Database migration finished in $duration milliseconds.",
        BOLD_TEXT,
      ),
    )
    println()
  }

  private fun MigrationState.possiblyHandleError() {
    val errorLocked = error ?: return

    when {
      !preRan ->
        println(
          colorize(
            "Unexpected error while running pre-migration code.",
            BRIGHT_RED_TEXT,
          ),
        )

      !ran -> {
        println(
          colorize(
            "Unexpected error while running migration.",
            BRIGHT_RED_TEXT,
          ),
        )

        // Report all executed commands in this migration.
        if (executedCommands.isNotEmpty()) {
          println()
          println(
            colorize(
              "These migration commands were already executed – " +
                "the last one probably caused the error:",
              BRIGHT_RED_TEXT,
            ),
          )
          println()
          for (command in executedCommands) {
            println("> $command")
          }
        }
      }

      !postRan ->
        println(
          colorize(
            "Unexpected error while running post-migration code.",
            BRIGHT_RED_TEXT,
          ),
        )

      else ->
        println(
          colorize(
            "Unexpected error while unlocking migrations table.",
            BRIGHT_RED_TEXT,
          ),
        )
    }

    println()

    throw errorLocked
  }

  private fun Throwable.capture(
    migrationState: MigrationState,
    message: String,
  ) {
    migrationState.error = this
    println(colorize(message, BRIGHT_RED_TEXT))
  }

  private fun java.sql.Savepoint.handleSavePoint(migrationState: MigrationState) {
    val shouldCommit = null == migrationState.error
    val operationHandler: (java.sql.Savepoint) -> Unit
    val operation: String

    if (shouldCommit) {
      operationHandler = databaseConnection::commit
      operation = "Committing"
    } else {
      operationHandler = databaseConnection::releaseSavePoint
      operation = "Rolling back"
    }

    try {
      print("    - $operation…")
      operationHandler(this)
      println(" Done.")
    } catch (ignored: java.sql.SQLException) {
      println()
      println(
        colorize(
          "Ignored an exception while ${operation.lowercase()}.",
          BOLD_TEXT,
        ),
      )
    }
  }

  @Throws(java.sql.SQLException::class)
  private fun Migration.performPre() {
    print("    - Running pre-migration code…")
    executePre(connection)
    println(" Done.")
  }

  @Throws(java.sql.SQLException::class)
  private fun Migration.performPost() {
    print("    - Running post-migration code…")
    executePost(connection)
    println(" Done.")
  }

  @Throws(java.sql.SQLException::class)
  private fun Migration.perform() {
    print("    - Running migration:")

    // Where the upgrade file resides.
    val upgradeFile = java.io.File(basePath, file)

    // Read all commands in the upgrade file.
    val upgradeCommands = upgradeFile.readFile()

    // Parse the upgrade commands.
    val commands = upgradeCommands.parseCommands()

    if (commands.isEmpty()) {
      print(" Skipped (empty)")
    } else {
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
    }

    println(".")
  }

  @Throws(java.sql.SQLException::class)
  private fun Migration.Versioned.lock() {
    print("    - Locking ${colorize(escapedTableName, BOLD_TEXT)} table…")

    val fieldValues = mapOf(
      escapedIdProperty to "$version",
      escapedDescriptionProperty to connection.escapeValue(description),
      escapedFileProperty to connection.escapeValue(file),
    )

    connection.execute(
      listOf(
        "INSERT INTO $escapedTableName (",
        fieldValues.keys.joinToString(separator = ","),
        ") VALUES (",
        fieldValues.values.joinToString(separator = ","),
        ")",
      ).joinToString(separator = ""),
    )

    println(" Done.")
  }

  @Throws(java.sql.SQLException::class)
  private fun Migration.Versioned.unlock() {
    print("    - Unlocking ${colorize(escapedTableName, BOLD_TEXT)} table… ")

    connection.execute(
      listOf(
        "UPDATE $escapedTableName",
        "SET $escapedCreatedProperty=${java.util.Date().secondsSinceEpoch()}",
        "WHERE $escapedIdProperty=$version",
      ).joinToString(separator = " "),
    )

    println(" Done.")
  }

  @Throws(java.io.IOException::class)
  private fun java.io.File.readFile(): Collection<String> = java.nio.file.Files
    .readAllLines(java.nio.file.Paths.get(toURI()))

  @Suppress("LoopWithTooManyJumpStatements")
  private fun Collection<String>.parseCommands(): Collection<String> =
    ArrayList<String>(Constants.MEDIUM_BLOCK_SIZE).also { result ->
      val nextCommand = StringBuilder(Constants.MEDIUM_BLOCK_SIZE)

      for (command in this) {
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
          result.add("$nextCommand")
          nextCommand.clear()
        } else {
          nextCommand.append(" $normalizedCommand")
        }
      }
    }

  @Suppress("VariableMinLength")
  private companion object {
    const val TABLE_NAME = "\$migration"
    const val ID: String = "migration_id"
    const val DESCRIPTION: String = "migration_description"
    const val FILE: String = "migration_file"
    const val CREATED: String = "migration_created"
    val DEFAULT_COMMENT_PREFIXES: Collection<String> = listOf("/*", "#", "-- ")
    val BOLD_TEXT: Attribute = Attribute.BOLD()
    val BRIGHT_GREEN_TEXT: Attribute = Attribute.BRIGHT_GREEN_TEXT()
    val BRIGHT_RED_TEXT: Attribute = Attribute.BRIGHT_RED_TEXT()
  }
}
