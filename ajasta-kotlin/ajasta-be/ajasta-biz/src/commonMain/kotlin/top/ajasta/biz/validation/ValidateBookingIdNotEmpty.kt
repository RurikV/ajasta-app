package top.ajasta.biz.validation

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.common.AjastaContext
import top.ajasta.common.models.AjastaBookingId

fun ICorChainDsl<AjastaContext>.validateBookingIdNotEmpty(title: String) = worker {
    this.title = title
    this.description = "Validates that booking id is not empty"
    on { bookingValidating.id == AjastaBookingId.NONE }
    handle {
        addError(
            code = "validation-id-empty",
            group = "validation",
            field = "id",
            message = "Booking ID must be provided"
        )
    }
}
