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
 * Holds a single row of results following a database query.
 *
 * In order to access the columns (fields), the caller needs to call the relevant getter method
 * (f.ex: [getString], [getInt], …) providing the name of the field.
 */
interface QueryRow {
  /**
   * Returns a string value for the provided field name.
   *
   * @param fieldName the field name to retrieve
   */
  fun getString(fieldName: String): String?

  /**
   * Returns a boolean value for the provided field name.
   *
   * @param fieldName the field name to retrieve
   */
  fun getBoolean(fieldName: String): Boolean

  /**
   * Returns a character value for the provided field name.
   *
   * @param fieldName the field name to retrieve
   */
  fun getChar(fieldName: String): Char

  /**
   * Returns a byte value for the provided field name.
   *
   * @param fieldName the field name to retrieve
   */
  fun getByte(fieldName: String): Byte

  /**
   * Returns an array of bytes for the provided field name.
   *
   * @param fieldName the field name to retrieve
   */
  fun getBytes(fieldName: String): ByteArray?

  /**
   * Returns a short value for the provided field name.
   *
   * @param fieldName the field name to retrieve
   */
  fun getShort(fieldName: String): Short

  /**
   * Returns an integer value for the provided field name.
   *
   * @param fieldName the field name to retrieve
   */
  fun getInt(fieldName: String): Int

  /**
   * Returns a long value for the provided field name.
   *
   * @param fieldName the field name to retrieve
   */
  fun getLong(fieldName: String): Long

  /**
   * Returns a float value for the provided field name.
   *
   * @param fieldName the field name to retrieve
   */
  fun getFloat(fieldName: String): Float

  /**
   * Returns a double value for the provided field name.
   *
   * @param fieldName the field name to retrieve
   */
  fun getDouble(fieldName: String): Double

  /**
   * Returns a date value for the provided field name.
   *
   * @param fieldName the field name to retrieve
   */
  fun getDate(fieldName: String): java.util.Date?
}
