package top.ajasta.biz.validation

import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaResourceId
import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker

fun ICorChainDsl<BizContext>.validateAvailabilityResourceIdNotEmpty(title: String) = worker {
    this.title = title
    this.description = "Validates that resource ID is provided for availability check"
    on { availabilityResourceId == AjastaResourceId.NONE }
    handle {
        addError(
            code = "validation-resource-id-empty",
            group = "validation",
            field = "resourceId",
            message = "Resource ID must be provided for availability check"
        )
    }
}
