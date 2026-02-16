package top.ajasta.common.models

import kotlin.jvm.JvmInline

/**
 * Type-safe wrapper for booking IDs.
 * Prevents mixing up IDs of different entity types.
 */
@JvmInline
value class AjastaBookingId(private val id: String) {
    fun asString() = id

    companion object {
        val NONE = AjastaBookingId("")
    }
}
