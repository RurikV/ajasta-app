package top.ajasta.biz.validation

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext

fun ICorChainDsl<BizContext>.validateSlotsNotEmpty(title: String) = worker {
    this.title = title
    this.description = "Validates that booking has at least one slot"
    on { bookingValidating.slots.isEmpty() }
    handle {
        addError(
            code = "validation-slots-empty",
            group = "validation",
            field = "slots",
            message = "At least one time slot must be provided"
        )
    }
}
