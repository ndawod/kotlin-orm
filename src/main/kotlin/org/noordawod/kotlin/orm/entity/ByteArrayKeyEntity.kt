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

import org.noordawod.kotlin.orm.extension.publicId
import org.noordawod.kotlin.security.ByteArrayStrength
import org.noordawod.kotlin.security.ByteUtils

/**
 * Generic top-level class for all entities having an [ByteArray].
 */
abstract class ByteArrayKeyEntity protected constructor() : BaseKeyEntity<ByteArray>() {
  override fun toString(): String = id.publicId()

  @Suppress("RedundantOverride")
  override fun hashCode(): Int = super.hashCode()

  override fun equals(other: Any?): Boolean =
    if (other is ByteArrayKeyEntity) id.contentEquals(other.id) else false

  companion object {
    /**
     * Just an empty [ByteArray] for reuse.
     */
    val EMPTY: ByteArray = byteArrayOf()

    /**
     * Generates a random [ByteArray] with length equal to specified value.
     */
    fun randomId(strength: ByteArrayStrength): ByteArray =
      randomId(
        strength.length
      )

    /**
     * Generates a random [ByteArray] with length equal to specified value.
     */
    fun randomId(length: Int): ByteArray = ByteUtils.randomBytes(length)
  }
}
