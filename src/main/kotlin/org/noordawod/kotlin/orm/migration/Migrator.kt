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

/**
 * Carriage-Return [Pattern][java.util.regex.Pattern] matcher.
 */
private val CARRIAGE_RETURN = java.util.regex.Pattern.compile("\\r")

/**
 * This class can migrate the database schema to its most recent defined version (above).
 */
@Suppress("TooManyFunctions")
class Migrator constructor(
  private val connection: Connection,
  private val tableName: String = TABLE_NAME,
  private val basePath: java.io.File
) {
  private var lastErroneousCommand: String? = null

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
  @Suppress("LongMethod")
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

      // Perform the migration while catching any errors.
      try {
        // Each migration runs in its own transaction so in case it fails, we can roll back.
        connection.execute("SET autocommit = 0")
        connection.execute("START TRANSACTION")

        // Kick it!
        performMigration(migration, migrationStart, nextVersion)

        // All seems normal, commit the result.
        performCommitOrRollback("COMMIT")
      } catch (error: java.sql.SQLException) {
        // An SQL error has occurred, we need to roll back all changes...
        performCommitOrRollback("ROLLBACK")

        // Delete this migration plan from the database as it hasn't been carried out.
        deleteMigration(migration)

        // If there was an exception, report the last command which caused the exception.
        val lastErroneousCommandLocked = lastErroneousCommand
        lastErroneousCommand = null
        if (null != lastErroneousCommandLocked) {
          println("")
          println("Unhandled exception while executing this SQL command:")
          println("")
          println(lastErroneousCommandLocked.trim { it <= ' ' })
          println("")
        }

        throw error
      } finally {
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
    } catch (@Suppress("TooGenericExceptionCaught") ignored: Throwable) {
      // We have an exception while committing the SQL commands; probably a bigger
      // problem in the database :/
      println("")
      println("Unhandled exception issuing a $command to finalize the migration plan.")
      println("")
    }
  }

  @Throws(java.sql.SQLException::class)
  private fun performMigration(
    migration: Migration,
    migrationStart: java.util.Date,
    nextVersion: Int
  ) {
    // Start with a clean slate.
    lastErroneousCommand = null

    // Lock this version in, or fail miserably otherwise.
    lockMigration(migration)

    // Debugging.
    print("  v$nextVersion [$migrationStart]: ${migration.description}:")

    try {
      // Where the upgrade file resides.
      val upgradeFile = java.io.File(basePath, migration.file)

      // Read all commands in the upgrade file.
      val upgradeCommands: String = readFile(upgradeFile)
        ?: throw java.sql.SQLException("Migration plan #$nextVersion is empty! ($upgradeFile)")

      // Execute the pre-execution code.
      migration.executePre(connection)

      // Parse the upgrade commands.
      val commands: List<String> = parseCommands(upgradeCommands)

      // Run the upgrade commands.
      var progress = 0
      var percent = 0
      for (command in commands) {
        lastErroneousCommand = command
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

      // Release the lock for this version.
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
        "'${escape(migration.description)}',",
        "'${escape(migration.file)}')"
      ).joinToString(separator = "")
    )
  }

  private fun escape(string: String): String = string.replace("'", "\\'")

  @Throws(java.sql.SQLException::class)
  private fun unlockMigration(migration: Migration) {
    connection.execute(
      "UPDATE `$tableName` SET " +
        "`${MigrationField.CREATED}`=${java.util.Date().secondsSinceEpoch()} WHERE " +
        "`${MigrationField.ID}`=${migration.version}"
    )
  }

  private fun deleteMigration(migration: Migration) {
    try {
      connection.execute(
        "DELETE FROM `$tableName` WHERE `${MigrationField.ID}`=${migration.version}"
      )
    } catch (@Suppress("TooGenericExceptionCaught") ignored: Throwable) {
    }
  }

  @Throws(java.io.IOException::class)
  private fun readFile(upgradeFile: java.io.File): String? {
    val bytes: ByteArray = java.nio.file.Files.readAllBytes(
      java.nio.file.Paths.get(upgradeFile.toURI())
    )
    return if (bytes.isEmpty()) null else String(bytes)
  }

  private fun parseCommands(commands: String): List<String> {
    return ArrayList<String>(1024).apply {
      // Flatten the SQL commands into a giant one-liner ending with a LF.
      val sqlDump = CARRIAGE_RETURN.matcher(commands).replaceAll(" ") + "\n"

      // Scan the string looking for individual commands ending with a semicolon.
      var startFrom = 0
      var semiColonPos: Int = sqlDump.indexOf(";\n", startFrom)
      while (startFrom < semiColonPos) {
        add(sqlDump.substring(startFrom, semiColonPos).trim { it <= ' ' })
        startFrom = 2 + semiColonPos
        semiColonPos = sqlDump.indexOf(";\n", startFrom)
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

  companion object {
    /**
     * Default name of the migrations table.
     */
    const val TABLE_NAME = "\$migrations"
  }
}
