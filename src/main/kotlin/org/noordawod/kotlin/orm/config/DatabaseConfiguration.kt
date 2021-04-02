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

@file:Suppress("unused")

package org.noordawod.kotlin.orm.config

import org.noordawod.kotlin.orm.MySQLDatabase

/**
 * Generic database configuration suitable for most database drivers.
 *
 * @param protocol associated URI protocol (scheme) for this JDBC driver
 * @param host host name of the database server
 * @param ipAddr IP address of the database server
 * @param port server's connection port
 * @param user username to authenticate against the database server
 * @param pass password to authenticate against the database server
 * @param schema main database schema name to attach to
 */
@kotlinx.serialization.Serializable
data class DatabaseConfiguration constructor(
  val protocol: String,
  val ipAddr: String,
  val host: String,
  val port: Int,
  val user: String,
  val pass: String,
  val schema: String
) {
  /**
   * Returns the connection URI string for this instance.
   */
  val uri: String = MySQLDatabase.uri(protocol, host, port, user, pass, schema)
}

/**
 * Generic database configuration for file-based migrations.
 *
 * @param protocol associated URI protocol (scheme) for this JDBC driver
 * @param ipAddr IP address of the database server
 * @param host host name of the database server
 * @param port server's connection port
 * @param user username to authenticate against the database server
 * @param pass password to authenticate against the database server
 * @param schema main database schema name to attach to
 * @param paths a list of paths indicating where the migrations plans are stored
 */
@kotlinx.serialization.Serializable
data class DatabaseMigrationConfiguration constructor(
  val protocol: String,
  val ipAddr: String,
  val host: String,
  val port: Int,
  val user: String,
  val pass: String,
  val schema: String,
  val paths: Collection<String>
) {
  /**
   * Returns the connection URI string for this instance.
   */
  val uri: String = MySQLDatabase.uri(protocol, host, port, user, pass, schema)
}

/**
 * Generic database configuration for pool-backed database server.
 *
 * @param protocol associated URI protocol (scheme) for this JDBC driver
 * @param ipAddr IP address of the database server
 * @param host host name of the database server
 * @param port server's connection port
 * @param user username to authenticate against the database server
 * @param pass password to authenticate against the database server
 * @param schema main database schema name to attach to
 * @param pool connection pool configuration
 */
@kotlinx.serialization.Serializable
data class DatabasePoolConfiguration constructor(
  val protocol: String,
  val ipAddr: String,
  val host: String,
  val port: Int,
  val user: String,
  val pass: String,
  val schema: String,
  val pool: PoolConfiguration
) {
  /**
   * Returns the connection URI string for this instance.
   */
  val uri: String = MySQLDatabase.uri(protocol, host, port, user, pass, schema)
}

/**
 * Database pool configuration.
 *
 * @param ageMillis how long, in milliseconds, to keep an idle connection open before closing it
 * @param maxFree how many concurrent open connections to keep open
 * @param healthCheckMillis how many milliseconds between connection health checks
 */
@kotlinx.serialization.Serializable
data class PoolConfiguration constructor(
  val ageMillis: Long,
  val maxFree: Int,
  val healthCheckMillis: Long
)
