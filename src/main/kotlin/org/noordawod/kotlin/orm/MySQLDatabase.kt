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
  "MemberVisibilityCanBePrivate",
  "LongParameterList",
  "unused",
)

package org.noordawod.kotlin.orm

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource
import com.j256.ormlite.jdbc.db.MysqlDatabaseType
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.support.DatabaseConnection
import net.moznion.uribuildertiny.URIBuilderTiny
import org.noordawod.kotlin.core.extension.mutableMapWith
import org.noordawod.kotlin.orm.config.DatabaseConfiguration

/**
 * A lightweight wrapper around JDBC's database driver.
 *
 * @param config database configuration to use
 * @param driver name of the JDBC database driver
 * @param ageMillis how long, in milliseconds, to keep an idle connection open before closing it
 * @param maxFree how many concurrent open connections to keep open
 * @param healthCheckMillis interval between health checks of the database connection
 * @param maxRetries maximum number of retries
 * @param retryDelay how long, in milliseconds, to wait between failed connection retries
 */
open class MySQLDatabase(
  config: DatabaseConfiguration,
  driver: String = JDBC_DRIVER,
  ageMillis: Long = DEFAULT_AGE_MILLIS,
  maxFree: Int = DEFAULT_MAX_FREE,
  healthCheckMillis: Long = DEFAULT_HEALTH_CHECK_INTERVAL,
  val maxRetries: Int = DEFAULT_RECONNECT_TRIES,
  val retryDelay: Long = DEFAULT_RETRY_MILLIS,
) : BaseDatabase(config, driver, ageMillis, maxFree, healthCheckMillis) {
  override fun equals(other: Any?): Boolean = other is MySQLDatabase &&
    super.equals(other) &&
    other.maxRetries == maxRetries &&
    other.retryDelay == retryDelay

  @Suppress("MagicNumber")
  override fun hashCode(): Int = super.hashCode() +
    maxRetries * 2713 +
    retryDelay.toInt() * 2801

  override val propertyWrapperChar: Char = '`'

  override val valueWrapperChar: Char = SINGLE_QUOTE_CHAR

  // Based on: https://rdr.to/EOBvOfkE8qj
  @Suppress("MagicNumber")
  override val escapeChars: CharArray = charArrayOf(
    '\b',
    '\n',
    '\r',
    '\t',
    '\\',
    SINGLE_QUOTE_CHAR,
    DOUBLE_QUOTE_CHAR,
    0x00.toChar(),
    0x1a.toChar(),
  )

  override val uri: String
    get() {
      var uriInternalLocked = uriInternal

      if (null == uriInternalLocked) {
        uriInternalLocked = uri(config)
        uriInternal = uriInternalLocked
      }

      return uriInternalLocked
    }

  private var uriInternal: String? = null

  /**
   * Returns the database connection URI string based on provided [config].
   *
   * @param config timezone to use in the server after connection
   * @return the final URI to connect to the JDBC database server
   *
   * @see <a href="https://tinyurl.com/yagm2clw">Connector/J Configuration Properties</a>
   */
  fun uri(config: DatabaseConfiguration): String = uri(
    protocol = config.protocol,
    host = config.host,
    port = config.port,
    user = config.user,
    pass = config.pass,
    schema = config.schema,
    timezone = config.timezone,
    collation = config.collation,
    connectTimeout = config.connectTimeout,
    socketTimeout = config.socketTimeout,
    params = config.params,
  )

  override fun initializeConnectionSource(): ConnectionSource {
    var retries = 0
    var error: java.sql.SQLException?

    do {
      try {
        val normalizedUrl = if (uri.startsWith(JDBC_PREFIX)) uri else "$JDBC_PREFIX$uri"
        val connectionSource = JdbcPooledConnectionSource(normalizedUrl, MysqlDatabaseType())

        connectionSource.setTestBeforeGet(true)
        connectionSource.setMaxConnectionAgeMillis(ageMillis)
        connectionSource.setMaxConnectionsFree(maxFree)
        connectionSource.setCheckConnectionsEveryMillis(healthCheckMillis)

        return connectionSource
      } catch (e: java.sql.SQLException) {
        error = e
      }

      Thread.sleep(retryDelay)
    } while (maxRetries > retries++)

    val retriesDebug = if (1 < maxRetries) "$maxRetries tries" else "$maxRetries try"
    val errorMessage = "Unable to connect to database after $retriesDebug: $uri"

    throw java.sql.SQLNonTransientConnectionException(errorMessage, error)
  }

  override fun enableForeignKeyChecks() {
    setForeignKeyChecks(connectionSource, true)
  }

  override fun disableForeignKeyChecks() {
    setForeignKeyChecks(connectionSource, false)
  }

  /**
   * Static functions, constants and other values.
   */
  companion object {
    /**
     * The character used to wrap field, table and database names in MySQL.
     */
    const val JDBC_DRIVER: String = "com.mysql.cj.jdbc.Driver"

    /**
     * Default collation to use in the database server.
     */
    const val DEFAULT_COLLATION: String = "utf8mb4_0900_ai_ci"

    /**
     * Default timezone to use in the database server.
     */
    const val DEFAULT_TIMEZONE: String = "UTC"

    private const val TRUE_STRING = "true"
    private const val FALSE_STRING = "false"

    /**
     * Returns the database connection URI string based on input parameters.
     *
     * @param protocol the associated URI protocol (scheme) for this JDBC driver
     * @param host host name of the database server
     * @param port database server's connection port
     * @param user username to authenticate against the database server
     * @param pass password to authenticate against the database server
     * @param schema main database schema name to attach to
     * @param timezone timezone to use in the server after connection
     * @param collation collation to choose after connecting to database server
     * @param connectTimeout timeout, in milliseconds, to wait for a connection
     * @param socketTimeout timeout, in milliseconds, on network socket operations
     * @return the final URI to connect to the JDBC database server
     *
     * @see <a href="https://tinyurl.com/yagm2clw">Connector/J Configuration Properties</a>
     */
    fun uri(
      protocol: String,
      host: String,
      port: Int,
      user: String,
      pass: String,
      schema: String,
      timezone: String? = null,
      collation: String? = null,
      connectTimeout: Long? = null,
      socketTimeout: Long? = null,
      params: Map<String, Any>? = null,
    ): String {
      @Suppress("MagicNumber")
      val eventualParams = mutableMapWith<String, Any>((params?.size ?: 16) + 16)

      // Base configuration.
      eventualParams["allowPublicKeyRetrieval"] = TRUE_STRING
      eventualParams["alwaysSendSetIsolation"] = FALSE_STRING
      eventualParams["autoReconnect"] = FALSE_STRING
      eventualParams["autoReconnectForPools"] = TRUE_STRING
      eventualParams["elideSetAutoCommits"] = TRUE_STRING
      eventualParams["enableQueryTimeouts"] = FALSE_STRING
      eventualParams["tcpKeepAlive"] = TRUE_STRING
      eventualParams["tcpNoDelay"] = TRUE_STRING
      eventualParams["useCompression"] = FALSE_STRING
      eventualParams["useLocalSessionState"] = TRUE_STRING
      eventualParams["useLocalTransactionState"] = TRUE_STRING
      eventualParams["useSSL"] = FALSE_STRING
      eventualParams["useUnicode"] = TRUE_STRING

      // Allow client to override a few parameters.
      if (!params.isNullOrEmpty()) {
        eventualParams.putAll(params)
      }

      // Continue configuration.
      eventualParams["user"] = user
      eventualParams["password"] = pass

      if (!timezone.isNullOrBlank()) {
        eventualParams["serverTimezone"] = timezone
      }
      if (!collation.isNullOrBlank()) {
        eventualParams["connectionCollation"] = collation
      }
      if (null != connectTimeout && 0L < connectTimeout) {
        eventualParams["connectTimeout"] = "$connectTimeout"
      }
      if (null != socketTimeout && 0L < socketTimeout) {
        eventualParams["socketTimeout"] = "$socketTimeout"
      }

      return URIBuilderTiny()
        .setScheme(protocol)
        .setHost(host)
        .setPort(port)
        .setPaths(schema)
        .setQueryParameters(eventualParams)
        .build()
        .toString()
    }

    /**
     * Disables foreign key checks.
     *
     * @param connectionSource the database connection source to use
     */
    fun enableForeignKeyChecks(connectionSource: ConnectionSource) {
      setForeignKeyChecks(connectionSource, true)
    }

    /**
     * Enables foreign key checks.
     *
     * @param connectionSource the database connection source to use
     */
    fun disableForeignKeyChecks(connectionSource: ConnectionSource) {
      setForeignKeyChecks(connectionSource, false)
    }

    /**
     * Changes foreign key checks state.
     *
     * @param connectionSource the database connection source to use
     * @param flag whether to enable checks (true) or not (false)
     */
    fun setForeignKeyChecks(
      connectionSource: ConnectionSource,
      flag: Boolean,
    ) {
      val databaseConnection = connectionSource.getReadWriteConnection("")

      try {
        databaseConnection.executeStatement(
          "SET FOREIGN_KEY_CHECKS=${if (flag) 1 else 0}",
          DatabaseConnection.DEFAULT_RESULT_FLAGS,
        )
      } finally {
        connectionSource.releaseConnection(databaseConnection)
      }
    }
  }
}
