package top.ajasta.biz.validation

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaResourceId

fun ICorChainDsl<BizContext>.validateResourceIdNotEmpty(title: String) = worker {
    this.title = title
    this.description = "Validates that resource id is not empty"
    on { resourceValidating.id == AjastaResourceId.NONE }
    handle {
        addError(
            code = "validation-id-empty",
            group = "validation",
            field = "id",
            message = "Resource ID must be provided"
        )
    }
}
