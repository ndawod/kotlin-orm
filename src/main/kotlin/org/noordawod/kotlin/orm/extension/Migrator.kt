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

package org.noordawod.kotlin.orm.extension

import org.noordawod.kotlin.orm.migration.MigrationConnection

/**
 * Escapes this LIKE value using the provided migrator connection.
 *
 * @param connection the migrator connection
 */
fun String.escapeLike(connection: MigrationConnection): String = connection
  .escapeLike(this)

/**
 * Escapes this database value using the provided migrator connection.
 *
 * @param connection the migrator connection
 */
fun String.escapeValue(connection: MigrationConnection): String = connection
  .escapeValue(this)

/**
 * Escapes these database values using the provided migrator connection.
 *
 * @param connection the migrator connection
 */
fun Collection<String>.escapeValues(connection: MigrationConnection): String = joinToString(
  separator = ",",
  transform = connection::escapeValue,
)

/**
 * Escapes this database property using the provided migrator connection.
 *
 * @param connection the migrator connection
 */
fun String.escapeProperty(connection: MigrationConnection): String = connection
  .escapeProperty(this)

/**
 * Escapes these database properties using the provided migrator connection.
 *
 * @param connection the migrator connection
 */
fun Collection<String>.escapeProperties(connection: MigrationConnection): String = joinToString(
  separator = ",",
  transform = connection::escapeProperty,
)
