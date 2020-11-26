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

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.SQLException
import java.util.regex.Pattern

/**
 * Carriage-Return [Pattern] matcher.
 */
private val CARRIAGE_RETURN = Pattern.compile("\\r")

/**
 * This class can migrate the database schema to its most recent defined version (above).
 */
@Suppress("TooManyFunctions")
class Migrator constructor(
  private val connection: Connection,
  private val tableName: String = TABLE_NAME,
  private val basePath: File
) {
  private var lastErroneousCommand: String? = null

  private val isLocked: Boolean
    get() {
      try {
        return 0L != connection.queryForLong(
          "SELECT COUNT(`${MigrationField.ID}`) AS count FROM `$tableName` " +
            "WHERE `${MigrationField.CREATED}` IS NULL"
        )
      } catch (ignored: SQLException) {
        // NO-OP.
      }
      return false
    }

  /**
   * Performs the migration steps.
   */
  @Throws(SQLException::class, IOException::class)
  fun execute(migrations: Array<Migration>) {
    // Make sure migrations table exists.
    ensureMigrationsTable()

    // Reusable variables.
    var isFirstRun = true

    // Get current version
    var nextVersion: Int = version()

    println("- Current version: $nextVersion")

    // Go over all migrations and run them one after the other.
    for (migration in migrations) {
      // Check if the migrations table is locked.
      if (isLocked) {
        throw SQLException("Migration table is locked, is another process active?")
      }

      // Get the next migration plan and check if it's already executed.
      if (migration.version <= nextVersion) {
        continue
      }

      // Migration plans must be continuous.
      nextVersion++
      if (migration.version != nextVersion) {
        // Either this is a migration we did before, or out of sync.
        throw SQLException(
          "Migration plan #$nextVersion is not continuous," +
            " database migration is out of sync!"
        )
      }

      // Debug.
      if (isFirstRun) {
        isFirstRun = false
        println("- Running migrations:")
      }

      // Error that may occur for this migration.
      var migrationError: SQLException? = null

      // Perform the upgrade command.
      try {
        performMigration(migration, nextVersion)
      } catch (e: SQLException) {
        migrationError = e
      } finally {
        performFinally(migration, migrationError)
      }
    }

    println("")
    println("Database Migration finished.")
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
    } catch (ignored: SQLException) {
    }
    return 0
  }

  @Throws(SQLException::class)
  private fun performMigration(migration: Migration, nextVersion: Int) {
    // Start with a clean slate.
    lastErroneousCommand = null

    // Start a new transaction.
    connection.execute("SET autocommit = 0")
    connection.execute("START TRANSACTION")

    // Lock this version in, or fail miserably otherwise.
    lockMigration(migration)

    // Debugging.
    print("  v$nextVersion: ${migration.description}:")

    // Where the upgrade file resides.
    val upgradeFile = File(basePath, migration.file)

    // Read all commands in the upgrade file.
    val upgradeCommands: String = readFile(upgradeFile)
      ?: throw SQLException("Migration plan #$nextVersion is empty! ($upgradeFile)")

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

    // Execute the post-execution code.
    migration.executePost(connection)

    // Release the lock for this version.
    unlockMigration(migration)
  }

  @Throws(SQLException::class)
  private fun performFinally(migration: Migration, migrationError: SQLException?) {
    println(".")
    // Issue the final command to either commit and rollback the transaction.
    val isCommit = null == migrationError
    try {
      connection.execute(if (isCommit) "COMMIT" else "ROLLBACK")
    } catch (e: SQLException) {
      // We have an exception while committing the SQL commands; probably a bigger
      // problem in the database :/
      if (isCommit) {
        println("")
        println(
          "Unhandled exception while committing the finalized migration plan. " +
            "This may indicate a bigger problem that manifests the database itself!"
        )
        println("")
        throw e
      }
    }

    // If an exception occurred, this is the last thing we do!
    if (null != migrationError) {
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
      throw migrationError
    }
  }

  @Throws(SQLException::class)
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

  @Throws(SQLException::class)
  private fun unlockMigration(migration: Migration) {
    connection.execute(
      "UPDATE `$tableName` SET " +
        "`${MigrationField.CREATED}`=${java.util.Date().time / 1000L} WHERE " +
        "`${MigrationField.ID}`=${migration.version}"
    )
  }

  private fun deleteMigration(migration: Migration) {
    connection.execute(
      "DELETE FROM `$tableName` WHERE `${MigrationField.ID}`=${migration.version}"
    )
  }

  @Throws(IOException::class)
  private fun readFile(upgradeFile: File): String? {
    val bytes: ByteArray = Files.readAllBytes(Paths.get(upgradeFile.toURI()))
    return if (bytes.isEmpty()) null else String(bytes)
  }

  private fun parseCommands(commands: String): List<String> {
    return ArrayList<String>(1024).apply {
      // Flatten the SQL commands into a giant one-liner ending with a LF.
      val sqlDump = CARRIAGE_RETURN.matcher(commands).replaceAll(" ") + "\n"

      // Scan the string looking for individual commands ending with a semi colon.
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
   * Creates the migrations table if it doesn't exist.
   */
  @Throws(SQLException::class)
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
    } catch (ignored: SQLException) {
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
