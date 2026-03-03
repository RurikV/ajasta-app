package top.ajasta.biz.validation

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext

fun ICorChainDsl<BizContext>.validateResourceNameLength(title: String) = worker {
    this.title = title
    this.description = "Validates that resource name has valid length"
    on { resourceValidating.name.length > 100 }
    handle {
        addError(
            code = "validation-name-length",
            group = "validation",
            field = "name",
            message = "Name must be at most 100 characters"
        )
    }
}
