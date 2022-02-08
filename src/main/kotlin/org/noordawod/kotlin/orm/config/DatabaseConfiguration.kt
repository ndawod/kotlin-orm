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

@file:Suppress("unused", "MemberVisibilityCanBePrivate", "LongParameterList")

package org.noordawod.kotlin.orm.config

import org.noordawod.kotlin.orm.MySQLDatabase
import kotlinx.serialization.Serializable

/**
 * Generic database configuration suitable for most database drivers.
 */
@Serializable
abstract class DatabaseConfiguration {
  /**
   * Associated URI protocol (scheme) for this JDBC driver.
   */
  abstract val protocol: String

  /**
   * IP address of the database server.
   */
  abstract val ipAddr: String

  /**
   * Host name of the database server.
   */
  abstract val host: String

  /**
   * Server's connection port.
   */
  abstract val port: Int

  /**
   * Username to authenticate against the database server.
   */
  abstract val user: String

  /**
   * Password to authenticate against the database server.
   */
  abstract val pass: String

  /**
   * Main database schema name to attach to.
   */
  abstract val schema: String

  /**
   * Returns the connection URI string for this instance.
   */
  @Suppress("LeakingThis")
  val uri: String = MySQLDatabase.uri(protocol, host, port, user, pass, schema)

  override fun equals(other: Any?): Boolean = other is DatabaseConfiguration &&
    other.protocol == protocol &&
    other.ipAddr == ipAddr &&
    other.host == host &&
    other.port == port &&
    other.user == user &&
    other.pass == pass &&
    other.schema == schema

  @Suppress("MagicNumber")
  override fun hashCode(): Int = port +
    protocol.hashCode() * 349 +
    ipAddr.hashCode() * 907 +
    host.hashCode() * 383 +
    user.hashCode() * 2087 +
    pass.hashCode() * 557 +
    schema.hashCode() * 1051

  /**
   * A default data class for [DatabaseConfiguration].
   */
  @Serializable
  data class Default constructor(
    override val protocol: String,
    override val ipAddr: String,
    override val host: String,
    override val port: Int,
    override val user: String,
    override val pass: String,
    override val schema: String
  ) : DatabaseConfiguration()
}

/**
 * Generic database configuration for file-based migrations.
 */
@Serializable
abstract class DatabaseMigrationConfiguration : DatabaseConfiguration() {
  /**
   * A list of paths indicating where the migrations plans are stored.
   */
  abstract val paths: Collection<String>

  override fun equals(other: Any?): Boolean = other is DatabaseMigrationConfiguration &&
    super.equals(other) &&
    other.paths == paths

  @Suppress("MagicNumber")
  override fun hashCode(): Int = super.hashCode() + paths.hashCode() * 181

  /**
   * A default data class for [DatabaseMigrationConfiguration].
   */
  @Serializable
  data class Default constructor(
    override val protocol: String,
    override val ipAddr: String,
    override val host: String,
    override val port: Int,
    override val user: String,
    override val pass: String,
    override val schema: String,
    override val paths: Collection<String>
  ) : DatabaseMigrationConfiguration()
}

/**
 * Generic database configuration for pool-backed database server.
 */
@Serializable
abstract class DatabasePoolConfiguration : DatabaseConfiguration() {
  /**
   * Connection pool configuration.
   */
  abstract val pool: PoolConfiguration

  override fun equals(other: Any?): Boolean = other is DatabasePoolConfiguration &&
    super.equals(other) &&
    other.pool == pool

  @Suppress("MagicNumber")
  override fun hashCode(): Int = super.hashCode() + pool.hashCode() * 181

  /**
   * A default data class for [DatabasePoolConfiguration].
   */
  @Serializable
  data class Default constructor(
    override val protocol: String,
    override val ipAddr: String,
    override val host: String,
    override val port: Int,
    override val user: String,
    override val pass: String,
    override val schema: String,
    override val pool: PoolConfiguration
  ) : DatabasePoolConfiguration()
}

/**
 * Database pool configuration.
 */
@Serializable
abstract class PoolConfiguration {
  /**
   * How long, in milliseconds, to keep an idle connection open before closing it.
   */
  abstract val ageMillis: Long

  /**
   * How many concurrent open connections to keep open.
   */
  abstract val maxFree: Int

  /**
   * How many milliseconds between connection health checks.
   */
  abstract val healthCheckMillis: Long

  override fun equals(other: Any?): Boolean = other is PoolConfiguration &&
    other.ageMillis == ageMillis &&
    other.maxFree == maxFree &&
    other.healthCheckMillis == healthCheckMillis

  @Suppress("MagicNumber")
  override fun hashCode(): Int = ageMillis.toInt() +
    maxFree * 349 +
    healthCheckMillis.toInt() * 907

  /**
   * A default data class for [PoolConfiguration].
   */
  @Serializable
  data class Default(
    override val ageMillis: Long,
    override val maxFree: Int,
    override val healthCheckMillis: Long
  ) : PoolConfiguration()
}
