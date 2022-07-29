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

package org.noordawod.kotlin.orm.query.impl

import com.j256.ormlite.support.DatabaseResults
import org.noordawod.kotlin.core.extension.mutableMapWith
import org.noordawod.kotlin.orm.query.QueryRow
import java.util.Date

internal class QueryRowImpl constructor(private val results: DatabaseResults) : QueryRow {
  private val columnsMap: Map<String, Int>

  init {
    mutableMapWith<String, Int>(results.columnCount).also { map ->
      results.columnNames.forEachIndexed { index, columnName ->
        map[columnName] = index
      }
      columnsMap = map
    }
  }

  override fun getString(fieldName: String): String? {
    TODO("Not yet implemented")
  }

  override fun getBoolean(fieldName: String): Boolean {
    TODO("Not yet implemented")
  }

  override fun getChar(fieldName: String): Char {
    TODO("Not yet implemented")
  }

  override fun getByte(fieldName: String): Byte {
    TODO("Not yet implemented")
  }

  override fun getBytes(fieldName: String): ByteArray? {
    TODO("Not yet implemented")
  }

  override fun getShort(fieldName: String): Short {
    TODO("Not yet implemented")
  }

  override fun getInt(fieldName: String): Int {
    TODO("Not yet implemented")
  }

  override fun getLong(fieldName: String): Long {
    TODO("Not yet implemented")
  }

  override fun getFloat(fieldName: String): Float {
    TODO("Not yet implemented")
  }

  override fun getDouble(fieldName: String): Double {
    TODO("Not yet implemented")
  }

  override fun getDate(fieldName: String): Date? {
    TODO("Not yet implemented")
  }
}
