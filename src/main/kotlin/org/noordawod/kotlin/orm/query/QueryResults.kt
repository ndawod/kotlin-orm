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

@file:Suppress("unused")

package org.noordawod.kotlin.orm.query

/**
 * Holds the results set following a database query.
 */
@Suppress("TooManyFunctions")
interface QueryResults : AutoCloseable {
  /**
   * Returns true if there is a row in the result set, false otherwise.
   */
  val hasNext: Boolean

  /**
   * Moves to the next row in the result set.
   */
  @Throws(java.sql.SQLException::class)
  fun next()

  /**
   * Returns a string value for the provided field name on success, null otherwise.
   *
   * @param fieldName the field name to retrieve
   */
  @Throws(java.sql.SQLException::class)
  fun getString(fieldName: String): String?

  /**
   * Returns a boolean value for the provided field name on success, null otherwise.
   *
   * @param fieldName the field name to retrieve
   */
  @Throws(java.sql.SQLException::class)
  fun getBoolean(fieldName: String): Boolean?

  /**
   * Returns a character value for the provided field name on success, null otherwise.
   *
   * @param fieldName the field name to retrieve
   */
  @Throws(java.sql.SQLException::class)
  fun getChar(fieldName: String): Char?

  /**
   * Returns a byte value for the provided field name on success, null otherwise.
   *
   * @param fieldName the field name to retrieve
   */
  @Throws(java.sql.SQLException::class)
  fun getByte(fieldName: String): Byte?

  /**
   * Returns an array of bytes for the provided field name on success, null otherwise.
   *
   * @param fieldName the field name to retrieve
   */
  @Throws(java.sql.SQLException::class)
  fun getBytes(fieldName: String): ByteArray?

  /**
   * Returns a short value for the provided field name on success, null otherwise.
   *
   * @param fieldName the field name to retrieve
   */
  @Throws(java.sql.SQLException::class)
  fun getShort(fieldName: String): Short?

  /**
   * Returns an integer value for the provided field name on success, null otherwise.
   *
   * @param fieldName the field name to retrieve
   */
  @Throws(java.sql.SQLException::class)
  fun getInt(fieldName: String): Int?

  /**
   * Returns a long value for the provided field name on success, null otherwise.
   *
   * @param fieldName the field name to retrieve
   */
  @Throws(java.sql.SQLException::class)
  fun getLong(fieldName: String): Long?

  /**
   * Returns a float value for the provided field name on success, null otherwise.
   *
   * @param fieldName the field name to retrieve
   */
  @Throws(java.sql.SQLException::class)
  fun getFloat(fieldName: String): Float?

  /**
   * Returns a double value for the provided field name on success, null otherwise.
   *
   * @param fieldName the field name to retrieve
   */
  @Throws(java.sql.SQLException::class)
  fun getDouble(fieldName: String): Double?

  /**
   * Returns a date value for the provided field name on success, null otherwise.
   *
   * @param fieldName the field name to retrieve
   */
  @Throws(java.sql.SQLException::class)
  fun getDate(fieldName: String): java.util.Date?
}
