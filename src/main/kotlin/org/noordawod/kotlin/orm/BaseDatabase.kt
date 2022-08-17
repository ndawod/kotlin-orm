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

import com.j256.ormlite.misc.TransactionManager
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.support.DatabaseConnection
import org.noordawod.kotlin.orm.config.DatabaseConfiguration

/**
 * A signature of a database connection handler obtained via [BaseDatabase.readOnlyConnection]
 * or [BaseDatabase.readWriteConnection].
 */
typealias DatabaseConnectionBlock<R> = (ConnectionSource) -> R

/**
 * A lightweight wrapper around JDBC's database driver.
 *
 * @param config database configuration to use
 * @param driver name of the JDBC database driver
 * @param ageMillis how long, in milliseconds, to keep an idle connection open before closing it
 * @param maxFree how many concurrent open connections to keep open
 * @param healthCheckMillis interval between health checks of the database connection
 */
@Suppress("TooManyFunctions", "LongParameterList")
abstract class BaseDatabase constructor(
  val config: DatabaseConfiguration,
  val driver: String? = null,
  val ageMillis: Long = DEFAULT_AGE_MILLIS,
  val maxFree: Int = DEFAULT_MAX_FREE,
  val healthCheckMillis: Long = DEFAULT_HEALTH_CHECK_INTERVAL
) {
  /**
   * The property holding the single [ConnectionSource].
   */
  protected val connectionSource: ConnectionSource

  init {
    if (null != driver) {
      try {
        Class.forName(driver).getDeclaredConstructor().newInstance()
      } catch (e: ClassNotFoundException) {
        throw java.sql.SQLException("Unable to locale a compatible database driver: $driver", e)
      } catch (e: IllegalAccessException) {
        throw java.sql.SQLException("Unable to load database driver: $driver", e)
      } catch (e: InstantiationException) {
        throw java.sql.SQLException("Unable to instantiate a database driver: $driver", e)
      }
    }

    @Suppress("LeakingThis")
    connectionSource = initialConnectionSource()
  }

  override fun equals(other: Any?): Boolean = other is BaseDatabase &&
    other.config == config &&
    other.driver == driver &&
    other.ageMillis == ageMillis &&
    other.maxFree == maxFree &&
    other.healthCheckMillis == healthCheckMillis

  @Suppress("MagicNumber")
  override fun hashCode(): Int = ageMillis.toInt() +
    config.hashCode() +
    driver.hashCode() * 907 +
    maxFree * 383 +
    healthCheckMillis.toInt() * 2087

  /**
   * The character used to wrap field, table and database names in this database server.
   */
  abstract val fieldWrapperChar: Char

  /**
   * The character used to escape values in this database server.
   */
  abstract val valueWrapperChar: Char

  /**
   * The double-quote character.
   */
  abstract val doubleQuoteChar: Char

  /**
   * The characters that require proper escaping before sending in a query.
   */
  abstract val escapeChars: CharArray

  /**
   * Returns the database connection URI string based on provided [config].
   */
  abstract val uri: String

  /**
   * Prints a list of database drivers currently loaded in the JVM and the ability of this
   * instance with [config] to connect to any of them.
   */
  fun showDatabaseDrivers() {
    showDatabaseDrivers(uri)
  }

  /**
   * Creates a new [ConnectionSource] to this [database][BaseDatabase] and executes [block]
   * with the successfully created source. After [block] is finished, with or without an error,
   * the connection is released.
   *
   * @param block the code block to run
   */
  @Throws(java.sql.SQLException::class)
  fun <R> readOnlyConnection(block: DatabaseConnectionBlock<R>): R {
    val connection: DatabaseConnection = connectionSource.getReadOnlyConnection("")
    try {
      return block(connectionSource)
    } finally {
      connectionSource.releaseConnection(connection)
    }
  }

  /**
   * Creates a new [ConnectionSource] to this [database][BaseDatabase] and executes [block]
   * with the successfully created source. After [block] is finished, with or without an error,
   * the connection is released.
   *
   * @param block the code block to run
   */
  @Throws(java.sql.SQLException::class)
  fun <R> readWriteConnection(block: DatabaseConnectionBlock<R>): R {
    val connection: DatabaseConnection = connectionSource.getReadWriteConnection("")
    try {
      return block(connectionSource)
    } finally {
      connectionSource.releaseConnection(connection)
    }
  }

  /**
   * Performs all database actions executed in the callback inside a transaction. If the
   * callback throws any exceptions, null will be returned.
   */
  @Throws(java.sql.SQLException::class)
  fun <R> transactional(callable: java.util.concurrent.Callable<R>): R =
    transactional(connectionSource, callable)

  /**
   * Performs all database actions executed in the callback while the specified table is
   * locked. If the callback throws any exceptions, NULL will be returned.
   */
  @Throws(java.sql.SQLException::class)
  fun <R> callWithLock(
    tableName: String,
    writeLock: Boolean,
    callable: java.util.concurrent.Callable<R>,
  ): R = callWithLock(connectionSource, tableName, writeLock, callable)

  /**
   * Shuts down the database pool of connections. Any subsequent attempt to use the pool
   * will produce an error.
   */
  @Throws(java.io.IOException::class)
  fun shutdown() {
    connectionSource.close()
  }

  /**
   * Escapes the provided string value and returns the escaped value.
   */
  fun escape(value: String, wrapper: Char?): String {
    val allow = fieldWrapperChar != wrapper
    return escape(value, wrapper, allow, allow)
  }

  /**
   * Escapes the provided string value and returns the escaped value.
   */
  @Suppress("MagicNumber", "ComplexMethod", "KotlinConstantConditions")
  fun escape(
    value: String,
    wrapper: Char?,
    alsoPercent: Boolean,
    alsoLowDash: Boolean,
  ): String {
    val length = value.length
    val builder = StringBuilder(value.length + 10)
    var arrayLength = escapeChars.size
    if (alsoPercent) {
      arrayLength++
    }
    if (alsoLowDash) {
      arrayLength++
    }

    // Create new array containing the characters to escape.
    val chars = CharArray(arrayLength)
    System.arraycopy(
      escapeChars,
      0,
      chars,
      0,
      escapeChars.size
    )
    var idx: Int = -1
    if (alsoPercent) {
      chars[++idx + escapeChars.size] = '%'
    }
    if (alsoLowDash) {
      chars[++idx + escapeChars.size] = '_'
    }
    idx = -1
    while (length > ++idx) {
      val valueChar = value[idx]
      @Suppress("ComplexCondition")
      if (
        null == wrapper ||
        (valueWrapperChar != wrapper || doubleQuoteChar != valueChar) &&
        (doubleQuoteChar != wrapper || valueWrapperChar != valueChar)
      ) {
        var found = false
        var specialCharIdx = -1
        while (!found && chars.size > ++specialCharIdx) {
          val thisChar = chars[specialCharIdx]
          found = valueChar == thisChar
        }
        if (found) {
          builder.append('\\')
        }
      }
      builder.append(valueChar)
    }

    return if (null == wrapper) {
      builder.toString()
    } else {
      wrapper.toString() + builder.toString() + wrapper
    }
  }

  /**
   * Builds and returns a "LIKE" SQL command for this database server.
   */
  fun like(word: String, start: Boolean, end: Boolean): String {
    var likeQuery = escape(word, null)
    if (start) {
      likeQuery = "%$likeQuery"
    }
    if (end) {
      likeQuery = "$likeQuery%"
    }
    return likeQuery
  }

  @Throws(java.sql.SQLException::class)
  protected abstract fun initialConnectionSource(): ConnectionSource

  companion object {
    /**
     * Initial capacity of arrays.
     */
    const val INITIAL_CAPACITY: Int = 50

    /**
     * Default maximum number of retries when inserting a new record.
     */
    const val DEFAULT_INSERT_TRIES: Int = 25

    /**
     * Default maximum number of retries when reconnecting to a database.
     */
    const val DEFAULT_RECONNECT_TRIES: Int = 5

    /**
     * Denotes just a one-try when inserting a new record.
     */
    const val ONE_TRY: Int = 1

    /**
     * How long, in milliseconds, to keep an idle connection open before closing it.
     */
    const val DEFAULT_AGE_MILLIS: Long = 2000L

    /**
     * How long, in milliseconds, to delay retrying a failed database operation.
     */
    const val DEFAULT_RETRY_MILLIS: Long = 5000L

    /**
     * How many concurrent open connections to keep open.
     */
    const val DEFAULT_MAX_FREE: Int = 10

    /**
     * Default duration between connection health checks.
     */
    const val DEFAULT_HEALTH_CHECK_INTERVAL: Long = 3000L

    /**
     * Matches one or more white-space characters.
     */
    val WHITE_SPACE: java.util.regex.Pattern = java.util.regex.Pattern.compile("\\s+")

    /**
     * Protocol used in the JVM for JDBC connections.
     */
    const val JDBC_PREFIX: String = "jdbc:"

    /**
     * Prints a list of database drivers currently loaded in the JVM. Used primarily for
     * debugging.
     */
    @Suppress("SwallowedException")
    fun showDatabaseDrivers(uri: String) {
      val drivers = java.sql.DriverManager.getDrivers()
      if (null == drivers || !drivers.hasMoreElements()) {
        System.err.println("No database drivers are registered!")
      } else {
        println("Checking registered database drivers against URI: $uri")
        while (drivers.hasMoreElements()) {
          val driver = drivers.nextElement()
          val acceptsURL = try {
            driver.acceptsURL(uri)
            true
          } catch (e: java.sql.SQLException) {
            false
          }
          println("  Class: " + driver.javaClass.name)
          println("  Version: " + driver.majorVersion + "." + driver.minorVersion)
          println("  Accepts URL? $acceptsURL")
        }
      }
    }

    /**
     * Performs all database actions executed in the callback inside a transaction. If the
     * callback throws any exceptions, null will be returned.
     */
    @Throws(java.sql.SQLException::class)
    fun <R> transactional(
      connection: ConnectionSource,
      callable: java.util.concurrent.Callable<R>,
    ): R = TransactionManager.callInTransaction(connection, callable)

    /**
     * Performs all database actions executed in the callback while the specified table is
     * locked. If the callback throws any exceptions, NULL will be returned.
     */
    @Suppress("KotlinConstantConditions")
    @Throws(java.sql.SQLException::class)
    fun <R> callWithLock(
      connection: ConnectionSource,
      tableName: String,
      writeLock: Boolean,
      callable: java.util.concurrent.Callable<R>,
    ): R {
      val writeConnection = connection.getReadWriteConnection(tableName)

      return try {
        writeConnection.isAutoCommit = false
        val lockType = if (writeLock) "WRITE" else "READ"
        writeConnection.executeStatement(
          "LOCK TABLES `$tableName` $lockType",
          DatabaseConnection.DEFAULT_RESULT_FLAGS
        )
        try {
          val result = callable.call()
          writeConnection.commit(null)
          result
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
          @Suppress("KotlinConstantConditions")
          throw java.sql.SQLException("Transaction callable threw non-SQL exception", e)
        }
      } finally {
        // Try to restore if we are in auto-commit mode.
        writeConnection.executeStatement(
          "UNLOCK TABLES",
          DatabaseConnection.DEFAULT_RESULT_FLAGS
        )
        writeConnection.isAutoCommit = true

        // We should clear aggressively.
        connection.clearSpecialConnection(writeConnection)
        connection.releaseConnection(writeConnection)
      }
    }
  }
}
