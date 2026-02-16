package top.ajasta.app.common

import top.ajasta.common.AjastaContext

/**
 * Interface for application settings.
 * Provides processor for handling business logic.
 */
interface IAjastaAppSettings {
    val processor: AjastaProcessor
}

/**
 * Processor interface for handling business logic.
 */
interface AjastaProcessor {
    suspend fun exec(ctx: AjastaContext)
}
