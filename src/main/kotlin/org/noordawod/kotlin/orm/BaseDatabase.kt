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

import com.diogonunes.jcolor.Ansi.colorize
import com.diogonunes.jcolor.Attribute
import com.j256.ormlite.misc.TransactionManager
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.support.DatabaseConnection
import org.noordawod.kotlin.core.extension.mutableListWith
import org.noordawod.kotlin.orm.config.DatabaseConfiguration

/**
 * A signature of a database connection handler obtained via [BaseDatabase.readOnlyConnection]
 * or [BaseDatabase.readWriteConnection].
 */
typealias DatabaseConnectionBlock<R> = ConnectionSource.(DatabaseConnection) -> R

/**
 * A lightweight wrapper around JDBC's database driver.
 *
 * @param config database configuration to use
 * @param driver name of the JDBC database driver
 * @param ageMillis how long, in milliseconds, to keep an idle connection open before closing it
 * @param maxFree how many concurrent open connections to keep open
 * @param healthCheckMillis interval between health checks of the database connection
 * @param maxReentrancy how many reentrant transactions are allowed
 */
@Suppress("TooManyFunctions", "LongParameterList")
abstract class BaseDatabase constructor(
  val config: DatabaseConfiguration,
  val driver: String? = null,
  val ageMillis: Long = DEFAULT_AGE_MILLIS,
  val maxFree: Int = DEFAULT_MAX_FREE,
  val healthCheckMillis: Long = DEFAULT_HEALTH_CHECK_INTERVAL,
  val maxReentrancy: Int = DEFAULT_MAX_REENTRANCY
) {
  private val connectionSourceLock = Object()
  private var connectionSourceInternal: ConnectionSource? = null
  private var transactionsEngaged: Int = 0

  /**
   * Returns the [ConnectionSource] associated with this database.
   */
  val connectionSource: ConnectionSource
    @Throws(java.sql.SQLException::class)
    get() {
      synchronized(connectionSourceLock) {
        var connectionSourceLocked = connectionSourceInternal

        if (null == connectionSourceLocked || !connectionSourceLocked.isOpen("")) {
          shutdown()
          connectionSourceLocked = initializeConnectionSource()
          connectionSourceInternal = connectionSourceLocked
        }

        return connectionSourceLocked
      }
    }

  /**
   * Determines whether to automatically create a transactional connection if a
   * [read-write connection][readWriteConnection] is requested.
   *
   * When this is turned on, only a single [read-write connection][readWriteConnection] is
   * allowed to exist, and when this is turned off, a maximum of [maxReentrancy] connections
   * can exist.
   *
   * By default, you need to call [transactional] in order to create a transactional read-write
   * connection.
   */
  var autoTransactional: Boolean = false

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
  }

  /**
   * Establishes a connection to the database and returns the resulting [ConnectionSource].
   */
  @Throws(java.sql.SQLException::class)
  abstract fun initializeConnectionSource(): ConnectionSource

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
   * The character used to wrap a property, such as a table name, column name, etc.
   */
  abstract val propertyWrapperChar: Char

  /**
   * The character used to escape column values.
   */
  abstract val valueWrapperChar: Char

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
   * Creates a new read-only [DatabaseConnection] to this [database][BaseDatabase] and
   * executes [block] with that connection.
   *
   * After [block] is finished, with or without an error, the connection is released.
   */
  @Throws(java.sql.SQLException::class)
  fun <R> readOnlyConnection(block: DatabaseConnectionBlock<R>): R =
    runDatabaseConnectionBlock(
      enableRetryOnError = true,
      readWrite = false,
      block = block
    )

  /**
   * Creates a new read-write [DatabaseConnection] to this [database][BaseDatabase] and
   * executes [block] with that connection.
   *
   * After [block] is finished, with or without an error, the connection is released.
   */
  @Throws(java.sql.SQLException::class)
  fun <R> readWriteConnection(block: DatabaseConnectionBlock<R>): R =
    readWriteConnection(
      enableRetryOnError = true,
      block = block
    )

  /**
   * Creates a new read-write [DatabaseConnection] to this [database][BaseDatabase] and
   * executes [block] with that connection.
   *
   * After [block] is finished, with or without an error, the connection is released.
   */
  @Throws(java.sql.SQLException::class)
  fun <R> readWriteConnection(
    enableRetryOnError: Boolean,
    block: DatabaseConnectionBlock<R>
  ): R = if (autoTransactional) {
    transactional(
      enableRetryOnError = enableRetryOnError,
      block = block
    )
  } else {
    runDatabaseConnectionBlock(
      enableRetryOnError = enableRetryOnError,
      readWrite = true,
      block = block
    )
  }

  /**
   * Creates a new read-only [DatabaseConnection] to this [database][BaseDatabase] and
   * executes [block] with that connection after locking [tableName] for read operations.
   *
   * After [block] is finished, with or without an error, the connection is released.
   */
  @Throws(java.sql.SQLException::class)
  fun <R> readOnlyLock(
    tableName: String,
    block: DatabaseConnectionBlock<R>
  ): R = readOnlyLock(
    tableName = tableName,
    enableRetryOnError = false,
    block = block
  )

  /**
   * Creates a new read-only [DatabaseConnection] to this [database][BaseDatabase] and
   * executes [block] with that connection after locking [tableName] for read operations.
   *
   * After [block] is finished, with or without an error, the connection is released.
   */
  @Throws(java.sql.SQLException::class)
  fun <R> readOnlyLock(
    tableName: String,
    enableRetryOnError: Boolean,
    block: DatabaseConnectionBlock<R>
  ): R = callWithLock(
    tableName = tableName,
    enableRetryOnError = enableRetryOnError,
    readWrite = false,
    block = block
  )

  /**
   * Creates a new read-write [DatabaseConnection] to this [database][BaseDatabase] and
   * executes [block] with that connection after locking [tableName] for read-write operations.
   *
   * After [block] is finished, with or without an error, the connection is released.
   */
  @Throws(java.sql.SQLException::class)
  fun <R> readWriteLock(
    tableName: String,
    enableRetryOnError: Boolean,
    block: DatabaseConnectionBlock<R>
  ): R = callWithLock(
    tableName = tableName,
    enableRetryOnError = enableRetryOnError,
    readWrite = true,
    block = block
  )

  /**
   * Creates a new transactional [DatabaseConnection] to this [database][BaseDatabase] and
   * executes [block] with that connection. If an error is thrown during execution of [block],
   * then the transaction is rolled back and not committed to the database.
   *
   * After [block] is finished, with or without an error, the connection is released.
   *
   * Note: Internally, the database connection is read-write always so the user operations may
   * modify the database freely.
   */
  @Throws(java.sql.SQLException::class)
  fun <R> transactional(block: DatabaseConnectionBlock<R>): R =
    transactional(
      enableRetryOnError = true,
      block = block
    )

  /**
   * Creates a new transactional [DatabaseConnection] to this [database][BaseDatabase] and
   * executes [block] with that connection. If an error is thrown during execution of [block],
   * then the transaction is rolled back and not committed to the database.
   *
   * After [block] is finished, with or without an error, the connection is released.
   *
   * Note: Internally, the database connection is read-write always so the user operations may
   * modify the database freely.
   */
  @Throws(java.sql.SQLException::class)
  fun <R> transactional(
    enableRetryOnError: Boolean,
    block: DatabaseConnectionBlock<R>
  ): R = runDatabaseConnectionBlock(
    enableRetryOnError = enableRetryOnError,
    readWrite = true,
  ) { databaseConnection ->
    if (0 < transactionsEngaged && autoTransactional) {
      throw java.sql.SQLException("Reentrant transaction detected.")
    }

    if (maxReentrancy == transactionsEngaged) {
      throw java.sql.SQLException("Maximum reentrant transactions reached: $maxReentrancy")
    }

    var saved = false

    try {
      transactionsEngaged++

      saved = saveSpecialConnection(databaseConnection)

      TransactionManager.callInTransaction(
        databaseConnection,
        saved,
        databaseType,
      ) {
        block(connectionSource, databaseConnection)
      }
    } finally {
      transactionsEngaged--

      if (saved) {
        clearSpecialConnection(databaseConnection)
      }
    }
  }

  /**
   * Shuts down the database pool of connections. Any subsequent attempt to use the pool
   * will produce an error.
   */
  @Throws(java.io.IOException::class)
  fun shutdown() {
    connectionSourceInternal?.closeQuietly()
    connectionSourceInternal = null
  }

  /**
   * Escapes the provided string property (table name, column name, etc.) and
   * returns the escaped value, wrapped inside [propertyWrapperChar] on both ends.
   */
  fun escapeProperty(name: String): String = escape(
    value = name,
    wrapper = propertyWrapperChar,
    escapePercent = false,
    escapeLowDash = false
  )

  /**
   * Escapes the provided string value and returns the escaped value, wrapped inside
   * [valueWrapperChar] on both ends.
   */
  fun escapeValue(value: String): String = escape(
    value = value,
    wrapper = valueWrapperChar,
    escapePercent = false,
    escapeLowDash = false
  )

  /**
   * Escapes the provided string used in a LIKE operation returns the escaped value,
   * as-is, wrapped inside [wrapper] on both ends, if provided of course.
   *
   * By default, [wrapper] is null.
   */
  fun escapeLike(value: String, wrapper: Char? = null): String = escape(
    value = value,
    wrapper = wrapper,
    escapePercent = true,
    escapeLowDash = true
  )

  /**
   * Builds and returns a "LIKE" SQL command for this database server.
   */
  fun like(word: String, start: Boolean, end: Boolean): String {
    var likeQuery = escapeLike(word)

    if (start) {
      likeQuery = "%$likeQuery"
    }

    if (end) {
      likeQuery = "$likeQuery%"
    }

    return likeQuery
  }

  /**
   * Escapes the provided string and returns the escaped value.
   *
   * @param value the string value to escape
   * @param wrapper the character to wrap the escaped value to return
   * @param escapePercent whether to escape the '%' character used mainly in LIKE clauses
   * @param escapeLowDash whether to escape the '_' character used mainly in LIKE clauses
   */
  @Suppress("KotlinConstantConditions")
  protected fun escape(
    value: String,
    wrapper: Char?,
    escapePercent: Boolean,
    escapeLowDash: Boolean
  ): String {
    @Suppress("MagicNumber")
    val arrayLength = escapeChars.size + 10

    val effectiveEscapeChars: MutableList<Char> = mutableListWith(arrayLength)
    for (escapeChar in escapeChars) {
      effectiveEscapeChars.add(escapeChar)
    }

    if (null != wrapper) {
      // The value of the wrapper character determines which of the single/double character
      // to escape – it's always the "other" one. So if wrapper=SINGLE_QUOTE_CHAR, then no
      // need to escape DOUBLE_QUOTE_CHAR, and vice versa.
      // However, when the wrapper is null, then both characters will be escaped.
      when (wrapper) {
        SINGLE_QUOTE_CHAR -> effectiveEscapeChars.remove(DOUBLE_QUOTE_CHAR)
        DOUBLE_QUOTE_CHAR -> effectiveEscapeChars.remove(SINGLE_QUOTE_CHAR)
      }
      effectiveEscapeChars.add(wrapper)
    }

    if (escapePercent) {
      effectiveEscapeChars.add('%')
    }

    if (escapeLowDash) {
      effectiveEscapeChars.add('_')
    }

    val builder = StringBuilder(value.length + arrayLength + arrayLength)

    for (valueChar in value) {
      if (effectiveEscapeChars.contains(valueChar)) {
        builder.append('\\')
      }
      builder.append(valueChar)
    }

    return if (null == wrapper) "$builder" else "$wrapper$builder$wrapper"
  }

  @Suppress("NestedBlockDepth")
  @Throws(java.sql.SQLException::class)
  private fun <R> runDatabaseConnectionBlock(
    enableRetryOnError: Boolean,
    readWrite: Boolean,
    block: DatabaseConnectionBlock<R>
  ): R {
    var shouldRetryOnError = enableRetryOnError
    var databaseConnection: DatabaseConnection? = null
    var latestError: Throwable?

    do {
      try {
        databaseConnection = if (readWrite) {
          connectionSource.getReadWriteConnection("")
        } else {
          connectionSource.getReadOnlyConnection("")
        }

        return block(connectionSource, databaseConnection)
      } catch (error: java.sql.SQLException) {
        latestError = error
        shouldRetryOnError = !shouldRetryOnError

        if (shouldRetryOnError) {
          shutdown()
        }
      } finally {
        if (null != databaseConnection) {
          connectionSource.releaseConnection(databaseConnection)
        }
      }
    } while (shouldRetryOnError)

    throw latestError ?: java.sql.SQLTransientConnectionException()
  }

  @Throws(java.sql.SQLException::class)
  private fun <R> callWithLock(
    tableName: String,
    enableRetryOnError: Boolean,
    readWrite: Boolean,
    block: DatabaseConnectionBlock<R>
  ): R = runDatabaseConnectionBlock(
    enableRetryOnError = enableRetryOnError,
    readWrite = readWrite
  ) { databaseConnection ->
    val isAutoCommit = databaseConnection.isAutoCommit
    val lockType = if (readWrite) "WRITE" else "READ"

    try {
      databaseConnection.isAutoCommit = false

      databaseConnection.executeStatement(
        "LOCK TABLES ${escapeProperty(tableName)} $lockType",
        DatabaseConnection.DEFAULT_RESULT_FLAGS
      )

      val result = block(this, databaseConnection)

      databaseConnection.commit(null)

      result
    } finally {
      // Try to restore if we are in auto-commit mode.
      databaseConnection.executeStatement(
        "UNLOCK TABLES",
        DatabaseConnection.DEFAULT_RESULT_FLAGS
      )
      databaseConnection.isAutoCommit = isAutoCommit
    }
  }

  companion object {
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
     * How many reentrant transactions is allowed.
     */
    const val DEFAULT_MAX_REENTRANCY: Int = 10

    /**
     * Default duration between connection health checks.
     */
    const val DEFAULT_HEALTH_CHECK_INTERVAL: Long = 3000L

    /**
     * Protocol used in the JVM for JDBC connections.
     */
    const val JDBC_PREFIX: String = "jdbc:"

    /**
     * The double-quote character.
     */
    const val DOUBLE_QUOTE_CHAR: Char = '"'

    /**
     * The double-quote character.
     */
    const val SINGLE_QUOTE_CHAR: Char = '\''

    /**
     * Prints a list of database drivers currently loaded in the JVM. Used primarily for
     * debugging.
     */
    @Suppress("SwallowedException")
    fun showDatabaseDrivers(uri: String) {
      val drivers = java.sql.DriverManager.getDrivers()
      val boldText = Attribute.BOLD()

      if (null == drivers || !drivers.hasMoreElements()) {
        System.err.println(colorize(
          "No database drivers are registered!",
          boldText
        ))
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

          println("  Class: " + colorize(driver.javaClass.name, boldText))
          println("  Version: " + colorize(
            "${driver.majorVersion}.${driver.minorVersion}",
            boldText
          ))
          println("  Accepts URL? " + colorize("$acceptsURL", boldText))
        }
      }
    }
  }
}
