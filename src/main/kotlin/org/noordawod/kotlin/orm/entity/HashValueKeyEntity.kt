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

import org.noordawod.kotlin.core.repository.HashValue
import org.noordawod.kotlin.core.security.ByteArrayStrength
import org.noordawod.kotlin.core.security.ByteUtils
import org.noordawod.kotlin.orm.extension.publicIdOrEmpty

/**
 * Generic top-level class for all entities having a [HashValue].
 */
abstract class HashValueKeyEntity protected constructor() : BaseKeyEntity<HashValue>() {
  override fun toString(): String = id.publicIdOrEmpty()

  override fun equals(other: Any?): Boolean =
    if (other is HashValueKeyEntity) id.contentEquals(other.id) else false

  @Suppress("RedundantOverride")
  override fun hashCode(): Int = super.hashCode()

  companion object {
    /**
     * Generates a random [HashValue] with a length corresponding with the provided [strength].
     *
     * @param strength determines the length of the [HashValue]
     */
    fun randomId(strength: ByteArrayStrength): HashValue = randomId(strength.length)

    /**
     * Generates a random [HashValue] with length equal to specified value.
     *
     * @param length length of [HashValue]
     */
    fun randomId(length: Int): HashValue = ByteUtils.randomBytes(length)
  }
}
