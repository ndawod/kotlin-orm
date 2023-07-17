/*
 * The MIT License
 *
 * Copyright 2023 Noor Dawod. All rights reserved.
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
 * Represents a condition used in a WHERE clause.
 *
 * Note: Field names and values must be already escaped.
 *
 * @param op optional operator when there are many conditions within this single Condition
 */
sealed class Condition(val op: LogicalOp?) {
  /**
   * A prepared condition to use as-is.
   *
   * @param value the pre-prepared condition
   */
  data class Prepared(
    val value: String
  ) : Condition(op = null) {
    override fun toString(): String = value
  }

  /**
   * A condition where a field's value is true.
   *
   * @param field the field name
   */
  data class True(
    val field: String
  ) : Condition(op = null) {
    override fun toString(): String = "$field = true"
  }

  /**
   * A condition where a field's value is false.
   *
   * @param field the field name
   */
  data class False(
    val field: String
  ) : Condition(op = null) {
    override fun toString(): String = "$field = false"
  }

  /**
   * A collection of conditions that all of them must evaluate to true (AND logic).
   *
   * @param values the list of conditions
   */
  data class AllOf(
    val values: Collection<Any>
  ) : Condition(op = LogicalOp.AND) {
    init {
      if (values.isEmpty()) {
        error("The list of conditions cannot be empty.")
      }
    }

    override fun toString(): String = values.joinToString(separator = " $op ")
  }

  /**
   * A collection of conditions that any of them may evaluate to true (OR logic).
   *
   * @param values the list of conditions
   */
  data class AnyOf(
    val values: Collection<Any>
  ) : Condition(op = LogicalOp.OR) {
    init {
      if (values.isEmpty()) {
        error("The list of conditions cannot be empty.")
      }
    }

    override fun toString(): String = values.joinToString(separator = " $op ")
  }

  /**
   * A condition where a field's value may evaluate to a value from a list of values.
   *
   * @param field the field name
   * @param values the list of values
   */
  data class In(
    val field: String,
    val values: Collection<Any>
  ) : Condition(op = null) {
    init {
      if (values.isEmpty()) {
        error("The list of values for '$field' cannot be empty.")
      }
    }

    override fun toString(): String = "$field IN (${values.joinToString(separator = ",")})"
  }

  /**
   * A condition where a field's value must not evaluate to a value from a list of values.
   *
   * @param field the field name
   * @param values the list of values
   */
  data class NotIn(
    val field: String,
    val values: Collection<Any>
  ) : Condition(op = null) {
    init {
      if (values.isEmpty()) {
        error("The list of values for '$field' cannot be empty.")
      }
    }

    override fun toString(): String = "$field NOT IN (${values.joinToString(separator = ",")})"
  }

  /**
   * A condition where a field's value is equal to a specific value.
   *
   * @param field the field name
   * @param value the specific value
   */
  data class Equals(
    val field: String,
    val value: Any
  ) : Condition(op = null) {
    override fun toString(): String = "$field = $value"
  }

  /**
   * A condition where a field's value is not equal to a specific value.
   *
   * @param field the field name
   * @param value the specific value
   */
  data class NotEquals(
    val field: String,
    val value: Any
  ) : Condition(op = null) {
    override fun toString(): String = "$field != $value"
  }

  /**
   * A condition where a field's value is between two numeric values (inclusive).
   *
   * Note: This equates to ([value1] <= [field] AND [field] <= [value2])
   *
   * @param field the field name
   * @param value1 the lower numeric value
   * @param value1 the upper numeric value
   */
  data class Between(
    val field: String,
    val value1: Number,
    val value2: Number
  ) : Condition(op = null) {
    override fun toString(): String = "$field BETWEEN $value1 AND $value2"
  }

  /**
   * A condition where a field's value is less than a specific value.
   *
   * @param field the field name
   * @param value the specific value
   */
  data class LessThan(
    val field: String,
    val value: Any
  ) : Condition(op = null) {
    override fun toString(): String = "$field < $value"
  }

  /**
   * A condition where a field's value is less than or equal to a specific value.
   *
   * @param field the field name
   * @param value the specific value
   */
  data class LessThanOrEqual(
    val field: String,
    val value: Number
  ) : Condition(op = null) {
    override fun toString(): String = "$field <= $value"
  }

  /**
   * A condition where a field's value is greater than a specific value.
   *
   * @param field the field name
   * @param value the specific value
   */
  data class GreaterThan(
    val field: String,
    val value: Number
  ) : Condition(op = null) {
    override fun toString(): String = "$field > $value"
  }

  /**
   * A condition where a field's value is greater than or equal to a specific value.
   *
   * @param field the field name
   * @param value the specific value
   */
  data class GreaterThanOrEqual(
    val field: String,
    val value: Number
  ) : Condition(op = null) {
    override fun toString(): String = "$field >= $value"
  }

  override fun hashCode(): Int = toString().hashCode()

  override fun equals(other: Any?): Boolean =
    other is Condition && other == this
}
