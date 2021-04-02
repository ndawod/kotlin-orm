/*
 * The MIT License
 *
 * Copyright 2020 Noor Dawod. All rights reserved.
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

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource
import com.j256.ormlite.jdbc.db.MysqlDatabaseType
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.support.DatabaseConnection
import net.moznion.uribuildertiny.URIBuilderTiny

/**
 * A lightweight wrapper around JDBC's database driver.
 *
 * @param config database configuration to use
 * @param driver name of the JDBC database driver
 * @param ageMillis how long, in milliseconds, to keep an idle connection open before closing it
 * @param maxFree how many concurrent open connections to keep open
 * @param healthCheckMillis interval between health checks of the database connection
 */
open class MySQLDatabase constructor(
  config: Configuration,
  driver: String = JDBC_DRIVER,
  ageMillis: Long = DEFAULT_AGE_MILLIS,
  maxFree: Int = DEFAULT_MAX_FREE,
  healthCheckMillis: Long = DEFAULT_HEALTH_CHECK_INTERVAL
) : BaseDatabase(config, driver, ageMillis, maxFree, healthCheckMillis) {
  override val fieldWrapperChar: Char = FIELD_WRAPPER_CHAR

  override val valueWrapperChar: Char = VALUE_WRAPPER_CHAR

  override val doubleQuoteChar: Char = DOUBLE_QUOTE_CHAR

  @Suppress("MagicNumber")
  override val escapeChars: CharArray = charArrayOf(
    '\\',
    '\n',
    '\r',
    VALUE_WRAPPER_CHAR,
    DOUBLE_QUOTE_CHAR,
    0x00.toChar(),
    0x1a.toChar()
  )

  override val uri: String = uri(config)

  /**
   * Returns the database connection URI string based on provided [config].
   *
   * @param config timezone to use in the server after connection
   * @return the final URI to connect to the JDBC database server
   *
   * @see <a href="https://tinyurl.com/yagm2clw">Connector/J Configuration Properties</a>
   */
  fun uri(config: Configuration): String = uri(
    config.protocol,
    config.host,
    config.port,
    config.user,
    config.pass,
    config.schema,
    config.collation,
    config.connectTimeout,
    config.socketTimeout,
    config.serverTimezone
  )

  override fun connectImpl(): ConnectionSource {
    val targetUri = if (uri.startsWith(JDBC_PREFIX)) uri else "$JDBC_PREFIX$uri"

    return JdbcPooledConnectionSource(targetUri, MysqlDatabaseType()).apply {
      setCheckConnectionsEveryMillis(healthCheckMillis)
      setMaxConnectionAgeMillis(ageMillis)
      setMaxConnectionsFree(maxFree)

      // Ensures that a connection is actually opened and a query is sent.
      var databaseConnection: DatabaseConnection? = null
      try {
        databaseConnection = getReadOnlyConnection(null).apply {
          executeStatement("SELECT NOW()", DatabaseConnection.DEFAULT_RESULT_FLAGS)
        }
      } finally {
        databaseConnection?.closeQuietly()
      }
    }
  }

  companion object {
    /**
     * The character used to wrap field, table and database names in MySQL.
     */
    const val JDBC_DRIVER: String = "com.mysql.cj.jdbc.Driver"

    /**
     * The character used to wrap field, table and database names in MySQL.
     */
    const val FIELD_WRAPPER_CHAR: Char = '`'

    /**
     * The character used to escape values in MySQL.
     */
    const val VALUE_WRAPPER_CHAR: Char = '\''

    /**
     * The double-quote character.
     */
    const val DOUBLE_QUOTE_CHAR: Char = '"'

    /**
     * Default collation to use in the database server.
     */
    const val DEFAULT_COLLATION: String = "utf8mb4_0900_ai_ci"

    /**
     * Default timezone to use in the database server.
     */
    const val DEFAULT_TIMEZONE: String = "UTC"

    /**
     * Returns the database connection URI string based on input parameters.
     *
     * @param protocol the associated URI protocol (scheme) for this JDBC driver
     * @param host host name of the database server
     * @param port database server's connection port
     * @param user username to authenticate against the database server
     * @param pass password to authenticate against the database server
     * @param schema main database schema name to attach to
     * @param collation collation to choose after connecting to database server
     * @param connectTimeout timeout in seconds to wait for a connection
     * @param socketTimeout timeout in seconds for the socket to connect
     * @param serverTimezone timezone to use in the server after connection
     * @return the final URI to connect to the JDBC database server
     *
     * @see <a href="https://tinyurl.com/yagm2clw">Connector/J Configuration Properties</a>
     */
    @Suppress("LongParameterList")
    fun uri(
      protocol: String,
      host: String,
      port: Int,
      user: String,
      pass: String,
      schema: String,
      collation: String? = null,
      connectTimeout: Long? = null,
      socketTimeout: Long? = null,
      serverTimezone: String? = null
    ): String {
      val params = mutableMapOf<String, Any>(
        "user" to user,
        "password" to pass,
        "useUnicode" to true.toString(),
        "allowPublicKeyRetrieval" to true.toString(),
        "useSSL" to false.toString(),
        "useCompression" to false.toString(),
        "autoReconnect" to true.toString()
      )
      if (!collation.isNullOrBlank()) {
        params["connectionCollation"] = collation
      }
      if (null != connectTimeout && 0L < connectTimeout) {
        params["connectTimeout"] = "$connectTimeout"
      }
      if (null != socketTimeout && 0L < socketTimeout) {
        params["socketTimeout"] = "$socketTimeout"
      }
      if (!serverTimezone.isNullOrBlank()) {
        params["serverTimezone"] = serverTimezone
      }
      return URIBuilderTiny()
        .setScheme(protocol)
        .setHost(host)
        .setPort(port)
        .setPaths(schema)
        .setQueryParameters(params)
        .build()
        .toString()
    }
  }
}
