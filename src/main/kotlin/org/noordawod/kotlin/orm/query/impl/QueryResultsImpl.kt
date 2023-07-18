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

@file:Suppress("JoinDeclarationAndAssignment")

package org.noordawod.kotlin.orm.query.impl

import com.j256.ormlite.support.CompiledStatement
import com.j256.ormlite.support.DatabaseResults
import org.noordawod.kotlin.core.extension.mutableMapWith

internal class QueryResultsImpl(
  private val statement: CompiledStatement
) : org.noordawod.kotlin.orm.query.QueryResults {
  @Suppress("IdentifierGrammar")
  private val columnsMap: Map<String, Int>
  private val results: DatabaseResults
  private var hasNextValue: Boolean

  init {
    results = statement.runQuery(null)
    columnsMap = mutableMapWith(results.columnCount)

    var index = -1
    for (columnName in results.columnNames) {
      columnsMap[columnName] = ++index
    }

    hasNextValue = results.first()
  }

  override val hasNext: Boolean get() = hasNextValue

  override fun close() {
    statement.closeQuietly()
    results.closeQuietly()
  }

  override fun next() {
    hasNextValue = results.next()
  }

  override fun getString(fieldName: String): String? {
    val columnIndex = columnsMap[fieldName]
    return if (null == columnIndex || 0 > columnIndex) null else results.getString(columnIndex)
  }

  override fun getBoolean(fieldName: String): Boolean? {
    val columnIndex = columnsMap[fieldName]
    return if (null == columnIndex || 0 > columnIndex) null else results.getBoolean(columnIndex)
  }

  override fun getChar(fieldName: String): Char? {
    val columnIndex = columnsMap[fieldName]
    return if (null == columnIndex || 0 > columnIndex) null else results.getChar(columnIndex)
  }

  override fun getByte(fieldName: String): Byte? {
    val columnIndex = columnsMap[fieldName]
    return if (null == columnIndex || 0 > columnIndex) null else results.getByte(columnIndex)
  }

  override fun getBytes(fieldName: String): ByteArray? {
    val columnIndex = columnsMap[fieldName]
    return if (null == columnIndex || 0 > columnIndex) null else results.getBytes(columnIndex)
  }

  override fun getShort(fieldName: String): Short? {
    val columnIndex = columnsMap[fieldName]
    return if (null == columnIndex || 0 > columnIndex) null else results.getShort(columnIndex)
  }

  override fun getInt(fieldName: String): Int? {
    val columnIndex = columnsMap[fieldName]
    return if (null == columnIndex || 0 > columnIndex) null else results.getInt(columnIndex)
  }

  override fun getLong(fieldName: String): Long? {
    val columnIndex = columnsMap[fieldName]
    return if (null == columnIndex || 0 > columnIndex) null else results.getLong(columnIndex)
  }

  override fun getFloat(fieldName: String): Float? {
    val columnIndex = columnsMap[fieldName]
    return if (null == columnIndex || 0 > columnIndex) null else results.getFloat(columnIndex)
  }

  override fun getDouble(fieldName: String): Double? {
    val columnIndex = columnsMap[fieldName]
    return if (null == columnIndex || 0 > columnIndex) null else results.getDouble(columnIndex)
  }

  override fun getDate(fieldName: String): java.util.Date? {
    val columnIndex = columnsMap[fieldName]
    return if (null == columnIndex || 0 > columnIndex) null else results.getTimestamp(columnIndex)
  }
}
