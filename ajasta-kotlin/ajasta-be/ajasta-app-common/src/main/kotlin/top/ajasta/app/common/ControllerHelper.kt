package top.ajasta.app.common

import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import top.ajasta.api.v1.models.Error
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaCommand
import top.ajasta.common.models.AjastaError
import top.ajasta.common.models.AjastaState
import top.ajasta.repo.inmemory.RepoBookingInMemory
import top.ajasta.repo.inmemory.RepoResourceInMemory

/**
 * Helper function for processing requests in controllers.
 * Handles logging, error handling, and stub processing.
 */
suspend inline fun <T> IAjastaAppSettings.controllerHelper(
    crossinline getRequest: suspend BizContext.() -> Unit,
    crossinline toResponse: suspend BizContext.() -> T,
    logId: String,
): T {
    val logger = LoggerFactory.getLogger("AjastaController")
    val ctx = BizContext(
        timeStart = Clock.System.now(),
    ).apply {
        // Initialize repositories (in production, these would be injected)
        repoBooking = RepoBookingInMemory()
        repoResource = RepoResourceInMemory()
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

/**
 * Converts AjastaError to transport Error model.
 */
fun AjastaError.toTransport() = Error(
    code = code.takeIf { it.isNotEmpty() },
    group = group.takeIf { it.isNotEmpty() },
    field = field.takeIf { it.isNotEmpty() },
    message = message.takeIf { it.isNotEmpty() }
)

/**
 * Converts list of errors to transport format.
 */
fun List<AjastaError>.toTransportErrors(): List<Error>? = this
    .map { it.toTransport() }
    .takeIf { it.isNotEmpty() }
