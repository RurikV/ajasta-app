package top.ajasta.biz.validation

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext

fun ICorChainDsl<BizContext>.validateBookingDescriptionLength(title: String) = worker {
    this.title = title
    this.description = "Validates that booking description has valid length"
    on { bookingValidating.description.length > 500 }
    handle {
        addError(
            code = "validation-description-length",
            group = "validation",
            field = "description",
            message = "Description must be at most 500 characters"
        )
    }
}
