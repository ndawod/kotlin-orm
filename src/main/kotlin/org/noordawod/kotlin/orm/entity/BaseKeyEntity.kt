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

package org.noordawod.kotlin.orm.entity

/**
 * Base ORM class for all tables that have a key (primary or unique).
 */
abstract class BaseKeyEntity<ID> protected constructor() : BaseEntity() {
  /**
   * Returns the unique ID of this ORM entity.
   */
  @Suppress("VariableMinLength")
  abstract var id: ID

  /**
   * Whether this entity has been populated from the database.
   */
  var populated: Boolean = false

  override fun toString(): String = id.toString()

  override fun hashCode(): Int = toString().hashCode()

  override fun equals(other: Any?): Boolean = (other as? BaseKeyEntity<*>)?.id == id

  companion object {
    /**
     * Just the "id" characters.
     */
    @Suppress("VariableMinLength")
    const val ID: String = "id"

    /**
     * Just the "_id" characters.
     */
    const val ID_SUFFIX: String = "_$ID"
  }
}
