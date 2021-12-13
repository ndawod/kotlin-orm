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
import org.noordawod.kotlin.orm.entity.PublicId
import org.noordawod.kotlin.security.base62

/**
 * Returns the corresponding [PublicId] for this [HashValue] on success,
 * null otherwise.
 */
fun HashValue?.publicId(): PublicId? = if (null == this || isEmpty()) null else base62()

/**
 * Returns the corresponding [PublicId] for this [HashValue] on success,
 * [fallback] otherwise.
 *
 * @param fallback value to return if this [HashValue] is null or empty
 */
fun HashValue?.publicIdOr(fallback: PublicId): PublicId = publicId() ?: fallback

/**
 * Returns the corresponding [PublicId] for this [HashValue] on success,
 * an empty PublicId otherwise.
 */
fun HashValue?.publicIdOrEmpty(): PublicId = publicIdOr("")

/**
 * Returns a new [Collection] that contains only non-null and non-empty [HashValue]s.
 */
fun Collection<HashValue?>?.filterNonEmpty(): Collection<HashValue>? =
  if (null == this) {
    null
  } else {
    val result = ArrayList<HashValue>(size)
    forEach { entry ->
      if (null != entry && entry.isNotEmpty()) {
        result.add(entry)
      }
    }
    if (result.isEmpty()) null else result
  }
