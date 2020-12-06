/*
 * COPYRIGHT (C) FINESWAP.COM AND OTHERS. ALL RIGHTS RESERVED.
 * UNAUTHORIZED DUPLICATION, MODIFICATION OR PUBLICATION IS PROHIBITED.
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF FINESWAP.COM.
 * THE COPYRIGHT NOTICE ABOVE DOES NOT EVIDENCE ANY ACTUAL OR
 * INTENDED PUBLICATION OF SUCH SOURCE CODE.
 */

package org.noordawod.kotlin.orm.entity

/**
 * Generic top-level class for all entities having a key with a [Number] value type.
 */
abstract class NumericKeyEntity<ID : Number> protected constructor() : BaseKeyEntity<ID>()
