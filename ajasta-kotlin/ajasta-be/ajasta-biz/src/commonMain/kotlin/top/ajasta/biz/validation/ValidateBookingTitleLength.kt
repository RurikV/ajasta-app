package top.ajasta.biz.validation

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext

fun ICorChainDsl<BizContext>.validateBookingTitleLength(title: String) = worker {
    this.title = title
    this.description = "Validates that booking title has valid length"
    on { bookingValidating.title.length > 100 }
    handle {
        addError(
            code = "validation-title-length",
            group = "validation",
            field = "title",
            message = "Title must be at most 100 characters"
        )
    }
}
