package top.ajasta.biz.validation

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState

fun ICorChainDsl<BizContext>.validateBookingTitleNotEmpty(title: String) = worker {
    this.title = title
    this.description = "Validates that booking title is not empty"
    on { bookingValidating.title.isEmpty() }
    handle {
        addError(
            code = "validation-title-empty",
            group = "validation",
            field = "title",
            message = "Title must not be empty"
        )
    }
}
