/*
 * The MIT License
 *
 * Copyright 2026 Noor Dawod. All rights reserved.
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

import org.noordawod.kotlin.core.util.ComparisonOp
import org.noordawod.kotlin.orm.query.Condition

/**
 * Returns a [Condition] based on a numerical value for this [ComparisonOp].
 *
 * @param field the target field for comparison
 * @param value the numeric value for comparison
 */
fun ComparisonOp?.toCondition(
  field: String,
  value: Number,
): Condition = when (this) {
  ComparisonOp.EQUAL, null -> Condition.Equals(
    field = field,
    value = value,
  )

  ComparisonOp.NOT_EQUAL -> Condition.NotEquals(
    field = field,
    value = value,
  )

  ComparisonOp.LESS_THAN -> Condition.LessThan(
    field = field,
    value = value,
  )

  ComparisonOp.LESS_THAN_OR_EQUAL -> Condition.LessThanOrEqual(
    field = field,
    value = value,
  )

  ComparisonOp.GREATER_THAN -> Condition.GreaterThan(
    field = field,
    value = value,
  )

  ComparisonOp.GREATER_THAN_OR_EQUAL -> Condition.GreaterThanOrEqual(
    field = field,
    value = value,
  )
}
