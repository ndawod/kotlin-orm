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
 * Base ORM class that extends all entities.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseEntity protected constructor() {
  /**
   * Static functions, constants and other values.
   */
  companion object {
    /**
     * Default character set for English-only values, such as enums and sets.
     */
    const val ISO_8859_1: String = "ISO-8859-1"

    /**
     * Prefix applied to all foreign keys in all tables.
     */
    const val FK_PREFIX: String = "fk_"

    /**
     * The field name used for storing an IP address.
     */
    const val IP_ADDR: String = "ip_addr"

    /**
     * The field name used for storing a geolocation latitude.
     */
    const val LATITUDE: String = "latitude"

    /**
     * The field name used for storing a geolocation longitude.
     */
    const val LONGITUDE: String = "longitude"

    /**
     * The field name used for storing the creation time of records.
     */
    const val CREATED: String = "created"

    /**
     * The field name used for storing the updated time of records.
     */
    const val UPDATED: String = "updated"

    /**
     * The field name used for distinguishing the active state of a record.
     */
    const val ACTIVE: String = "active"
  }
}
