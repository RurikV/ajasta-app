package top.ajasta.common.models

import kotlin.jvm.JvmInline

/**
 * Type-safe wrapper for request IDs.
 * Used for request tracing and logging.
 */
@JvmInline
value class AjastaRequestId(private val id: String) {
    fun asString() = id

    companion object {
        val NONE = AjastaRequestId("")
    }
}
