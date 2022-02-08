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

package org.noordawod.kotlin.orm.extension

import org.noordawod.kotlin.orm.BaseDatabase
import org.noordawod.kotlin.orm.config.DatabaseConfiguration
import org.noordawod.kotlin.orm.config.DatabaseMigrationConfiguration
import org.noordawod.kotlin.orm.config.DatabasePoolConfiguration

/**
 * Generates a [configuration][BaseDatabase.Configuration] suitable for [BaseDatabase]'s
 * instantiation from a [DatabaseConfiguration] instance.
 */
@Suppress("DuplicatedCode")
fun DatabaseConfiguration.config(
  collation: String,
  connectTimeout: Long,
  socketTimeout: Long,
  serverTimezone: String
): BaseDatabase.Configuration = object : BaseDatabase.Configuration {
  override val protocol: String = this@config.protocol
  override val host: String = this@config.host
  override val port: Int = this@config.port
  override val user: String = this@config.user
  override val pass: String = this@config.pass
  override val schema: String = this@config.schema
  override val collation: String = collation
  override val connectTimeout: Long = connectTimeout
  override val socketTimeout: Long = socketTimeout
  override val serverTimezone: String = serverTimezone
}

/**
 * Generates a [configuration][BaseDatabase.Configuration] suitable for [BaseDatabase]'s
 * instantiation from a [DatabaseMigrationConfiguration] instance.
 */
@Suppress("DuplicatedCode")
fun DatabaseMigrationConfiguration.config(
  collation: String,
  connectTimeout: Long,
  socketTimeout: Long,
  serverTimezone: String
): BaseDatabase.Configuration = object : BaseDatabase.Configuration {
  override val protocol: String = this@config.protocol
  override val host: String = this@config.host
  override val port: Int = this@config.port
  override val user: String = this@config.user
  override val pass: String = this@config.pass
  override val schema: String = this@config.schema
  override val collation: String = collation
  override val connectTimeout: Long = connectTimeout
  override val socketTimeout: Long = socketTimeout
  override val serverTimezone: String = serverTimezone
}

/**
 * Generates a [configuration][BaseDatabase.Configuration] suitable for [BaseDatabase]'s
 * instantiation from a [DatabasePoolConfiguration] instance.
 */
@Suppress("DuplicatedCode")
fun DatabasePoolConfiguration.config(
  collation: String,
  connectTimeout: Long,
  socketTimeout: Long,
  serverTimezone: String
): BaseDatabase.Configuration = object : BaseDatabase.Configuration {
  override val protocol: String = this@config.protocol
  override val host: String = this@config.host
  override val port: Int = this@config.port
  override val user: String = this@config.user
  override val pass: String = this@config.pass
  override val schema: String = this@config.schema
  override val collation: String = collation
  override val connectTimeout: Long = connectTimeout
  override val socketTimeout: Long = socketTimeout
  override val serverTimezone: String = serverTimezone
}

/**
 * Default equals() implementation for [Configuration][BaseDatabase.Configuration] interface.
 *
 * @param other the other instance to compare this Configuration instance to
 */
fun BaseDatabase.Configuration.interfaceEquals(other: Any?): Boolean =
  other is BaseDatabase.Configuration &&
    other.protocol == protocol &&
    other.host == host &&
    other.port == port &&
    other.user == user &&
    other.pass == pass &&
    other.schema == schema &&
    other.collation == collation &&
    other.connectTimeout == connectTimeout &&
    other.socketTimeout == socketTimeout &&
    other.serverTimezone == serverTimezone

/**
 * Default hashCode() implementation for [Configuration][BaseDatabase.Configuration] interface.
 */
@Suppress("MagicNumber")
fun BaseDatabase.Configuration.interfaceHashCode(): Int = port +
  protocol.hashCode() * 349 +
  host.hashCode() * 383 +
  user.hashCode() * 2087 +
  pass.hashCode() * 557 +
  schema.hashCode() * 1051 +
  collation.hashCode() * 181 +
  connectTimeout.hashCode() * 881 +
  socketTimeout.hashCode() * 461 +
  serverTimezone.hashCode() * 89
