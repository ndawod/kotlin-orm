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

package org.noordawod.kotlin.orm.extension

import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.support.DatabaseConnection
import org.noordawod.kotlin.orm.BaseDatabase

/**
 * A signature of a database connection handler obtained via [BaseDatabase.readOnlyConnection]
 * or [BaseDatabase.readWriteConnection].
 */
typealias DatabaseConnectionBlock<R> = (ConnectionSource) -> R

/**
 * Creates a new [ConnectionSource] to this [database][BaseDatabase] and executes [block]
 * with the successfully created source. After [block] is finished, with or without an error,
 * the connection is released.
 *
 * @param block the code block to run
 */
@Throws(java.sql.SQLException::class)
inline fun <R> BaseDatabase.readOnlyConnection(
  maxRetries: Int = BaseDatabase.DEFAULT_RECONNECT_TRIES,
  retryDelay: Long = BaseDatabase.DEFAULT_RETRY_MILLIS,
  block: DatabaseConnectionBlock<R>
): R {
  var source: ConnectionSource? = null
  var connection: DatabaseConnection? = null
  try {
    source = connect(maxRetries, retryDelay)
    connection = source.getReadOnlyConnection("")
    return block(source)
  } finally {
    if (null != connection) {
      source?.releaseConnection(connection)
    }
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
inline fun <R> BaseDatabase.readWriteConnection(
  maxRetries: Int = BaseDatabase.DEFAULT_RECONNECT_TRIES,
  retryDelay: Long = BaseDatabase.DEFAULT_RETRY_MILLIS,
  block: DatabaseConnectionBlock<R>
): R {
  var source: ConnectionSource? = null
  var connection: DatabaseConnection? = null
  try {
    source = connect(maxRetries, retryDelay)
    connection = source.getReadWriteConnection("")
    return block(source)
  } finally {
    if (null != connection) {
      source?.releaseConnection(connection)
    }
  }
}
