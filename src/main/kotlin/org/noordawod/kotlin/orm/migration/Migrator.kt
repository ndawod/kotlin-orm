/*
 * COPYRIGHT (C) FINESWAP.COM AND OTHERS. ALL RIGHTS RESERVED.
 * UNAUTHORIZED DUPLICATION, MODIFICATION OR PUBLICATION IS PROHIBITED.
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF FINESWAP.COM.
 * THE COPYRIGHT NOTICE ABOVE DOES NOT EVIDENCE ANY ACTUAL OR
 * INTENDED PUBLICATION OF SUCH SOURCE CODE.
 */

@file:Suppress("unused", "MagicNumber")

package org.noordawod.kotlin.orm.migration

import org.noordawod.kotlin.core.extension.secondsSinceEpoch
import org.noordawod.kotlin.orm.query.QueryCommands
import org.noordawod.kotlin.orm.query.QueryResult

/**
 * This class can migrate the database schema to its most recent defined version (above).
 *
 * @param connection the Connection implementation to use for querying the database
 * @param tableName the table name to store meta-data about migrations
 * @param basePath base path to where the Migrator will find migration plans
 * @param commentPrefixes list of prefixes used for commenting migration plans
 */
@Suppress("TooManyFunctions")
class Migrator constructor(
  private val connection: Connection,
  private val tableName: String = TABLE_NAME,
  private val basePath: java.io.File,
  private val commentPrefixes: Collection<String> = DEFAULT_COMMENT_PREFIXES
) {
  private val isLocked: Boolean
    get() {
      try {
        return 0L != connection.queryForLong(
          "SELECT COUNT(`${MigrationField.ID}`) AS count FROM `$tableName` " +
            "WHERE `${MigrationField.CREATED}` IS NULL"
        )
      } catch (ignored: java.sql.SQLException) {
        // NO-OP.
      }
      return false
    }

  /**
   * Performs the migration steps.
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

    // Go over all migrations and run them one after the other.
    for (migration in migrations) {
      // Check if the migrations' table is locked.
      if (isLocked) {
        throw java.sql.SQLException("Migration table is locked, is another process active?")
      }

      // Get the next migration plan and check if it's already executed.
      if (migration.version <= nextVersion) {
        continue
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

      // Keep track of timing for this migration.
      val migrationStart = java.util.Date()

      val executedCommands = QueryCommands(128)

      // Perform the migration while catching any errors.
      try {
        // Each migration runs in its own transaction so in case it fails, we can roll back.
        connection.execute("SET autocommit = 0")
        connection.execute("START TRANSACTION")

        // Kick it!
        performMigration(executedCommands, migration, migrationStart, nextVersion)

        // All seems normal, commit the result.
        performCommitOrRollback("COMMIT")
      } catch (error: java.sql.SQLException) {
        // An SQL error has occurred, we need to roll back all changes...
        performCommitOrRollback("ROLLBACK")

        println("")
        println("Unexpected error while running migration v$nextVersion.")

        // Report all executed commands in this migration.
        if (executedCommands.isNotEmpty()) {
          println(
            "These migration commands were already executed – " +
              "the last one probably caused the error:"
          )
          println("")
          for (command in executedCommands) {
            println("> $command")
          }
        }

        println("")

        throw error
      } finally {
        executedCommands.clear()
        val migrationEnd = java.util.Date()
        println("  - Ended: $migrationEnd")
        println("  - Duration: ${migrationEnd.time - migrationStart.time} milliseconds")
      }
    }

    val migrationsEnd = java.util.Date()

    println("- Ended: $migrationsEnd")
    println("- Duration: ")
    println("")
    println(
      "Database Migration finished in " +
        "${migrationsEnd.time - migrationsStart.time} milliseconds."
    )
    println("")
  }

  /**
   * Returns the latest version of the database.
   */
  @Suppress("MemberVisibilityCanBePrivate")
  fun version(): Int {
    try {
      return connection.queryForLong(
        "SELECT MAX(`${MigrationField.ID}`) AS max_version FROM `$tableName`"
      ).toInt()
    } catch (ignored: java.sql.SQLException) {
    }
    return 0
  }

  private fun performCommitOrRollback(command: String) {
    try {
      connection.execute(command)
    } catch (ignored: java.sql.SQLException) {
      println("")
      println("Unhandled exception issuing a $command to finalize the migration plan.")
    }
  }

  @Throws(java.sql.SQLException::class)
  private fun performMigration(
    executedCommands: QueryCommands,
    migration: Migration,
    migrationStart: java.util.Date,
    nextVersion: Int
  ) {
    // Lock this migration.
    lockMigration(migration)

    // Debugging.
    print("  v$nextVersion [$migrationStart]: ${migration.description}:")

    try {
      // Where the upgrade file resides.
      val upgradeFile = java.io.File(basePath, migration.file)

      // Read all commands in the upgrade file.
      val upgradeCommands = readFile(upgradeFile)
        ?: throw java.sql.SQLException("Migration plan #$nextVersion is empty! ($upgradeFile)")

      // Execute the pre-execution code.
      migration.executePre(connection)

      // Parse the upgrade commands.
      val commands = parseCommands(upgradeCommands)

      // Run the upgrade commands.
      var progress = 0
      var percent = 0
      for (command in commands) {
        executedCommands.add(command)
        connection.execute(command)
        progress++
        val nextPercent = (progress.toFloat() / commands.size.toFloat() * 100f).toInt()
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

      // Execute the post-execution code.
      migration.executePost(connection)

      // Release the lock for this migration.
      unlockMigration(migration)
    } finally {
      println("")
    }
  }

  @Throws(java.sql.SQLException::class)
  private fun lockMigration(migration: Migration) {
    connection.execute(
      arrayOf(
        "INSERT INTO `$tableName` (",
        "`${MigrationField.ID}`,",
        "`${MigrationField.DESCRIPTION}`,",
        "`${MigrationField.FILE}`",
        ") VALUES (",
        "${migration.version},",
        "'${connection.escapeValue(migration.description)}',",
        "'${connection.escapeValue(migration.file)}')"
      ).joinToString(separator = "")
    )
  }

  @Throws(java.sql.SQLException::class)
  private fun unlockMigration(migration: Migration) {
    connection.execute(
      "UPDATE `$tableName` SET " +
        "`${MigrationField.CREATED}`=${java.util.Date().secondsSinceEpoch()} WHERE " +
        "`${MigrationField.ID}`=${migration.version}"
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
        "CREATE TABLE `$tableName` (" +
          "`${MigrationField.ID}` smallint UNSIGNED NOT NULL PRIMARY KEY," +
          "`${MigrationField.DESCRIPTION}` tinytext CHARACTER SET ascii NOT NULL," +
          "`${MigrationField.FILE}` tinytext CHARACTER SET ascii NOT NULL," +
          "`${MigrationField.CREATED}` int UNSIGNED NULL" +
          ")"
      )
      connection.execute("ALTER TABLE `$tableName` ADD KEY (`${MigrationField.CREATED}`)")
      println("- Migrations table missing, auto-created...")
    } catch (ignored: java.sql.SQLException) {
      println("- Migrations table exists: $tableName (v${version()})")
      return
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
    fun query(statement: String): QueryResult

    /**
     * Queries the database with the specified [statement] and returns the first result as a [Long].
     *
     * @param statement the database statement for the query
     */
    @Throws(java.sql.SQLException::class)
    fun queryForLong(statement: String): Long

    /**
     * Escape a value so it can be safely used a value in an SQL statement.
     *
     * @param value the value to escape
     */
    fun escapeValue(value: String): String
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
