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

@file:Suppress("unused", "MemberVisibilityCanBePrivate", "LongParameterList")

package org.noordawod.kotlin.orm.config

import kotlinx.serialization.Serializable
import org.noordawod.kotlin.orm.MySQLDatabase

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
   * Server's connection port.
   */
  abstract val port: Int

  /**
   * Host name of the database server.
   */
  abstract val host: String

  /**
   * Timezone of the host.
   */
  abstract val timezone: String

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
   * Collation to use for this host.
   */
  abstract val collation: String?

  /**
   * Timeout, in milliseconds, for a client to connect to the host.
   */
  abstract val connectTimeout: Long?

  /**
   * Timeout, in milliseconds, on network socket operations to the host.
   */
  abstract val socketTimeout: Long?

  /**
   * Returns the connection URI string for this instance.
   */
  val uri: String
    get() =
      uriInternal ?: MySQLDatabase.uri(protocol, host, port, user, pass, schema).apply {
        uriInternal = this
      }

  private var uriInternal: String? = null

  override fun equals(other: Any?): Boolean = other is DatabaseConfiguration &&
    other.protocol == protocol &&
    other.ipAddr == ipAddr &&
    other.port == port &&
    other.host == host &&
    other.timezone == timezone &&
    other.user == user &&
    other.pass == pass &&
    other.schema == schema &&
    other.collation == collation &&
    other.connectTimeout == connectTimeout &&
    other.socketTimeout == socketTimeout

  @Suppress("MagicNumber")
  override fun hashCode(): Int = port +
    protocol.hashCode() * 349 +
    ipAddr.hashCode() * 907 +
    host.hashCode() * 383 +
    timezone.hashCode() * 293 +
    user.hashCode() * 2087 +
    pass.hashCode() * 557 +
    schema.hashCode() * 1051 +
    collation.hashCode() * 709 +
    connectTimeout.hashCode() * 199 +
    socketTimeout.hashCode() * 503

  /**
   * A default data class for [DatabaseConfiguration].
   */
  @Serializable
  data class Default constructor(
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
    override val socketTimeout: Long? = null
  ) : DatabaseConfiguration()

  companion object {
    /**
     * How long, in milliseconds, until a client can connect to a server.
     */
    const val DEFAULT_CONNECT_TIMEOUT: Long = 2000L
  }
}
