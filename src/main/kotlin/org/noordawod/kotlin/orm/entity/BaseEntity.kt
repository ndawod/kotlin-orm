/*
 * COPYRIGHT (C) FINESWAP.COM AND OTHERS. ALL RIGHTS RESERVED.
 * UNAUTHORIZED DUPLICATION, MODIFICATION OR PUBLICATION IS PROHIBITED.
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF FINESWAP.COM.
 * THE COPYRIGHT NOTICE ABOVE DOES NOT EVIDENCE ANY ACTUAL OR
 * INTENDED PUBLICATION OF SUCH SOURCE CODE.
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.noordawod.kotlin.orm.entity

/**
 * Base ORM class to describe all entities.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseEntity protected constructor() {
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
