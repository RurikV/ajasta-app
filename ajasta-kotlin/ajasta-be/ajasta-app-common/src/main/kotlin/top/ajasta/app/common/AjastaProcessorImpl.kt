package top.ajasta.app.common

import top.ajasta.biz.AjastaProcessor as BizProcessor
import top.ajasta.common.AjastaContext

/**
 * Implementation of AjastaProcessor that uses the business logic module.
 * This is the main processor that should be used in production.
 */
class AjastaProcessorImpl : AjastaProcessor {
    private val bizProcessor = BizProcessor()

    override suspend fun exec(ctx: AjastaContext) {
        bizProcessor.exec(ctx)
    }
}
