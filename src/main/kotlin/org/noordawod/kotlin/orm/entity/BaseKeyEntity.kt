/*
 * COPYRIGHT (C) FINESWAP.COM AND OTHERS. ALL RIGHTS RESERVED.
 * UNAUTHORIZED DUPLICATION, MODIFICATION OR PUBLICATION IS PROHIBITED.
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF FINESWAP.COM.
 * THE COPYRIGHT NOTICE ABOVE DOES NOT EVIDENCE ANY ACTUAL OR
 * INTENDED PUBLICATION OF SUCH SOURCE CODE.
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package org.noordawod.kotlin.orm.entity

/**
 * The type of a public record identifier.
 */
typealias PublicId = String

/**
 * Base ORM class for all tables that have a key (primary or unique).
 */
abstract class BaseKeyEntity<ID> protected constructor() : BaseEntity() {
  /**
   * Returns the unique ID of this ORM entity.
   */
  @Suppress("VariableMinLength")
  abstract var id: ID

  override fun toString(): String = id.toString()

  override fun hashCode(): Int = toString().hashCode()

  override fun equals(other: Any?): Boolean = (other as? BaseKeyEntity<*>)?.id == id

  companion object {
    /**
     * Just the "id" characters that appears at the end of keys.
     */
    @Suppress("VariableMinLength")
    const val ID: String = "id"

    /**
     * The "id" suffix applied to keys with an underscore.
     */
    const val ID_SUFFIX: String = "_$ID"
  }
}
