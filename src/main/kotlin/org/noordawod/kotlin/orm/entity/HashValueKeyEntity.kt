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

import org.noordawod.kotlin.orm.extension.publicIdOrEmpty
import org.noordawod.kotlin.security.ByteArrayStrength
import org.noordawod.kotlin.security.ByteUtils

/**
 * Generic top-level class for all entities having a [HashValue].
 */
abstract class HashValueKeyEntity protected constructor() : BaseKeyEntity<HashValue>() {
  override fun toString(): String = id.publicIdOrEmpty()

  override fun equals(other: Any?): Boolean =
    if (other is HashValueKeyEntity) id.contentEquals(other.id) else false

  @Suppress("RedundantOverride")
  override fun hashCode(): Int = super.hashCode()

  companion object {
    /**
     * Generates a random [HashValue] with a length corresponding with the provided [strength].
     *
     * @param strength determines the length of the [HashValue]
     */
    fun randomId(strength: ByteArrayStrength): HashValue = randomId(strength.length)

    /**
     * Generates a random [HashValue] with length equal to specified value.
     *
     * @param length length of [HashValue]
     */
    fun randomId(length: Int): HashValue = ByteUtils.randomBytes(length)
  }
}
