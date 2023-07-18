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

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.noordawod.kotlin.orm.query

/**
 * Represents a condition used in a WHERE clause.
 *
 * Note: Field names and values must already be escaped and matching the target
 * database escape grammar.
 *
 * @param op optional operator when there are many conditions within this single Condition
 * @param parenthesized whether the eventual query will include surrounding parentheses
 */
sealed class Condition private constructor(
  val op: LogicalOp? = null,
  val parenthesized: Boolean = false
) {
  /**
   * Evaluates to true if this condition is valid, false otherwise.
   */
  abstract val isValid: Boolean

  /**
   * Returns the textual representation of this condition.
   */
  protected abstract fun stringify(): String

  final override fun toString(): String {
    val result = stringify()
    return if (parenthesized) "($result)" else result
  }

  final override fun hashCode(): Int = toString().hashCode()

  final override fun equals(other: Any?): Boolean = other is Condition && other == this

  /**
   * A prepared condition to use as-is.
   *
   * @param value the pre-prepared condition
   */
  class Prepared(
    val value: String,
    parenthesized: Boolean = false
  ) : Condition(parenthesized = parenthesized) {
    override val isValid: Boolean = value.isNotBlank()

    override fun stringify(): String = value
  }

  /**
   * A condition where a field's value is true.
   *
   * @param field the field name
   */
  class True(
    val field: String
  ) : Condition() {
    override val isValid: Boolean = field.isNotBlank()

    override fun stringify(): String = "$field = true"
  }

  /**
   * A condition where a field's value is false.
   *
   * @param field the field name
   */
  class False(
    val field: String
  ) : Condition() {
    override val isValid: Boolean = field.isNotBlank()

    override fun stringify(): String = "$field = false"
  }

  /**
   * A collection of conditions that all of them must evaluate to true (AND logic).
   *
   * @param values the list of conditions
   */
  class AllOf(
    val values: Collection<Any>,
    parenthesized: Boolean = false
  ) : Condition(
    op = LogicalOp.AND,
    parenthesized = parenthesized
  ) {
    override val isValid: Boolean = values.isNotEmpty() && values.none { "$it".isBlank() }

    override fun stringify(): String = values
      .filterNot { "$it".isBlank() }
      .joinToString(separator = " $op ")
  }

  /**
   * A collection of conditions that any of them may evaluate to true (OR logic).
   *
   * @param values the list of conditions
   */
  class AnyOf(
    val values: Collection<Any>,
    parenthesized: Boolean = false
  ) : Condition(
    op = LogicalOp.OR,
    parenthesized = parenthesized
  ) {
    override val isValid: Boolean = values.isNotEmpty() && values.none { "$it".isBlank() }

    override fun stringify(): String = values
      .filterNot { "$it".isBlank() }
      .joinToString(separator = " $op ")
  }

  /**
   * A condition where a bunch of fields may match against a textual query.
   *
   * Note: This is a MySQL-specific query and may or may not work in other databases.
   *
   * @param fields the list of fields
   * @param value the textual query value
   * @param mode the full-text search mode
   */
  class MatchAgainst(
    val fields: Collection<String>,
    val value: String,
    val mode: MatchAgainstMode
  ) : Condition() {
    override val isValid: Boolean = fields.isNotEmpty() && value.isNotBlank()

    override fun stringify(): String =
      "MATCH (${fields.joinToString(separator = ",")}) AGAINST ($value $mode)"
  }

  /**
   * A condition where a field's value may evaluate to a value from a list of values.
   *
   * @param field the field name
   * @param values the list of values
   */
  class In(
    val field: String,
    val values: Collection<Any>
  ) : Condition() {
    override val isValid: Boolean = field.isNotBlank() && values.isNotEmpty()

    override fun stringify(): String = "$field IN (${values.joinToString(separator = ",")})"
  }

  /**
   * A condition where a field's value must not evaluate to a value from a list of values.
   *
   * @param field the field name
   * @param values the list of values
   */
  class NotIn(
    val field: String,
    val values: Collection<Any>
  ) : Condition() {
    override val isValid: Boolean = field.isNotBlank() && values.isNotEmpty()

    override fun stringify(): String = "$field NOT IN (${values.joinToString(separator = ",")})"
  }

  /**
   * A condition where a field's value is equal to a specific value.
   *
   * @param field the field name
   * @param value the specific value
   */
  class Equals(
    val field: String,
    val value: Any
  ) : Condition() {
    override val isValid: Boolean = field.isNotBlank() && value.toString().isNotBlank()

    override fun stringify(): String = "$field = $value"
  }

  /**
   * A condition where a field's value is not equal to a specific value.
   *
   * @param field the field name
   * @param value the specific value
   */
  class NotEquals(
    val field: String,
    val value: Any
  ) : Condition() {
    override val isValid: Boolean = field.isNotBlank() && value.toString().isNotBlank()

    override fun stringify(): String = "$field != $value"
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
  class Between(
    val field: String,
    val value1: Number,
    val value2: Number
  ) : Condition() {
    override val isValid: Boolean = field.isNotBlank()

    override fun stringify(): String = "$field BETWEEN $value1 AND $value2"
  }

  /**
   * A condition where a field's value is less than a specific value.
   *
   * @param field the field name
   * @param value the specific value
   */
  class LessThan(
    val field: String,
    val value: Any
  ) : Condition() {
    override val isValid: Boolean = field.isNotBlank() && value.toString().isNotBlank()

    override fun stringify(): String = "$field < $value"
  }

  /**
   * A condition where a field's value is less than or equal to a specific value.
   *
   * @param field the field name
   * @param value the specific value
   */
  class LessThanOrEqual(
    val field: String,
    val value: Number
  ) : Condition() {
    override val isValid: Boolean = field.isNotBlank()

    override fun stringify(): String = "$field <= $value"
  }

  /**
   * A condition where a field's value is greater than a specific value.
   *
   * @param field the field name
   * @param value the specific value
   */
  class GreaterThan(
    val field: String,
    val value: Number
  ) : Condition() {
    override val isValid: Boolean = field.isNotBlank()

    override fun stringify(): String = "$field > $value"
  }

  /**
   * A condition where a field's value is greater than or equal to a specific value.
   *
   * @param field the field name
   * @param value the specific value
   */
  class GreaterThanOrEqual(
    val field: String,
    val value: Number
  ) : Condition() {
    override val isValid: Boolean = field.isNotBlank()

    override fun stringify(): String = "$field >= $value"
  }
}
