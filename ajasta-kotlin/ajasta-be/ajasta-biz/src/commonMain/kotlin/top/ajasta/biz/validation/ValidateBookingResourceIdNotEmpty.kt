package top.ajasta.biz.validation

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaResourceId

fun ICorChainDsl<BizContext>.validateBookingResourceIdNotEmpty(title: String) = worker {
    this.title = title
    this.description = "Validates that booking resourceId is provided"
    on { bookingValidating.resourceId == AjastaResourceId.NONE }
    handle {
        addError(
            code = "validation-resourceId-empty",
            group = "validation",
            field = "resourceId",
            message = "Resource ID must be provided"
        )
    }
}
