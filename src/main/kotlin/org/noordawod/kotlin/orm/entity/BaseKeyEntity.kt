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
 * The type of public record identifier.
 */
typealias PublicId = String

/**
 * The type of internal record identifiers.
 */
typealias HashValue = ByteArray

/**
 * Corresponds to a [HashValue] that's 0-byte.
 */
val EmptyHashValue: HashValue = byteArrayOf()

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
