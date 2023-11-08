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

package org.noordawod.kotlin.orm.config

import kotlinx.serialization.Serializable

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
  data class Default(
    override val protocol: String,
    override val ipAddr: String,
    override val port: Int,
    override val host: String,
    override val timezone: String,
    override val user: String,
    override val pass: String,
    override val schema: String,
    override val collation: String? = null,
    override val connectTimeout: Long? = DEFAULT_CONNECT_TIMEOUT,
    override val socketTimeout: Long? = null,
    override val pool: PoolConfiguration,
    override val params: Map<String, String>? = null,
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
    override val healthCheckMillis: Long,
  ) : PoolConfiguration()
}
