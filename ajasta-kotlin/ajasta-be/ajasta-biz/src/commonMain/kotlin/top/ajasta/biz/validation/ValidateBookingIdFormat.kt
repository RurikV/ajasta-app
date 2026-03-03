package top.ajasta.biz.validation

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaBookingId

fun ICorChainDsl<BizContext>.validateBookingIdFormat(title: String) = worker {
    this.title = title
    this.description = "Validates that booking id has proper format"
    val regex = Regex("^[a-zA-Z0-9-]+$")
    on { bookingValidating.id != AjastaBookingId.NONE && !bookingValidating.id.asString().matches(regex) }
    handle {
        addError(
            code = "validation-id-format",
            group = "validation",
            field = "id",
            message = "Booking ID must contain only alphanumeric characters and hyphens"
        )
    }
}
