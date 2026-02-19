package top.ajasta.app.common

import top.ajasta.biz.BizContext

/**
 * Interface for application settings.
 * Provides processor for handling business logic.
 * Repositories should be provided by the context or via extension.
 */
interface IAjastaAppSettings {
    val processor: AjastaProcessor
}

/**
 * Processor interface for handling business logic.
 */
interface AjastaProcessor {
    suspend fun exec(ctx: BizContext)
}
