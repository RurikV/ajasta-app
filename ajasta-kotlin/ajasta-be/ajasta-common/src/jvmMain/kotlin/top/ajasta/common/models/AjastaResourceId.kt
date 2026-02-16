package top.ajasta.common.models

import kotlin.jvm.JvmInline

/**
 * Type-safe wrapper for resource IDs.
 * Prevents mixing up IDs of different entity types.
 */
@JvmInline
value class AjastaResourceId(private val id: String) {
    fun asString() = id

    companion object {
        val NONE = AjastaResourceId("")
    }
}
