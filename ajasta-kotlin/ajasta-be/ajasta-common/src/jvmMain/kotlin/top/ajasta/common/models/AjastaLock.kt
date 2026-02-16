package top.ajasta.common.models

import kotlin.jvm.JvmInline

/**
 * Type-safe wrapper for optimistic locking version.
 * Used for concurrent modification detection.
 */
@JvmInline
value class AjastaLock(private val lock: String) {
    fun asString() = lock

    companion object {
        val NONE = AjastaLock("")
    }
}
