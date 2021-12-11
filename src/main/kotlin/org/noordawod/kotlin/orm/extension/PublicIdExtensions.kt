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

import org.noordawod.kotlin.orm.entity.HashValue
import org.noordawod.kotlin.orm.entity.HashValueKeyEntity
import org.noordawod.kotlin.orm.entity.PublicId
import org.noordawod.kotlin.security.base62

/**
 * Returns the corresponding [HashValue] for this [PublicId] on success, [fallback] otherwise.
 *
 * @param fallback value to return if this [PublicId] is null or empty
 */
fun PublicId?.hashValue(fallback: HashValue = HashValueKeyEntity.EMPTY): HashValue =
  if (true == this?.isNotEmpty()) base62() else fallback

/**
 * Returns the corresponding [HashValue] for this [PublicId] on success, null otherwise.
 */
fun PublicId?.optionalHashValue(): HashValue? = hashValue().let {
  if (it.isEmpty()) null else it
}
