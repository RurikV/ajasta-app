package top.ajasta.app.common

import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaCommand
import top.ajasta.common.models.AjastaError
import top.ajasta.common.models.AjastaState
import top.ajasta.repo.IRepoBooking
import top.ajasta.repo.IRepoResource

/**
 * Helper function for processing requests in controllers.
 * Handles logging, error handling, and stub processing.
 * Uses provided repositories to ensure singleton/shared instances.
 */
suspend inline fun <T> IAjastaAppSettings.controllerHelper(
    crossinline getRequest: suspend BizContext.() -> Unit,
    crossinline toResponse: suspend BizContext.() -> T,
    logId: String,
    repoBooking: IRepoBooking,
    repoResource: IRepoResource,
): T {
    val logger = LoggerFactory.getLogger("AjastaController")
    val ctx = BizContext(
        timeStart = Clock.System.now(),
    ).apply {
        this.repoBooking = repoBooking
        this.repoResource = repoResource
    }
    return try {
        ctx.getRequest()
        logger.info("Request $logId started: command=${ctx.command}")
        processor.exec(ctx)
        logger.info("Request $logId processed: state=${ctx.state}")
        ctx.toResponse()
    } catch (e: Throwable) {
        logger.error("Request $logId failed", e)
        ctx.state = AjastaState.FAILING
        ctx.errors.add(
            AjastaError(
                code = "internal-error",
                group = "system",
                field = "",
                message = e.message ?: "Unknown error",
                exception = e
            )
        )
        if (ctx.command == AjastaCommand.NONE) {
            ctx.command = AjastaCommand.NONE
        }
        ctx.toResponse()
    }
}
