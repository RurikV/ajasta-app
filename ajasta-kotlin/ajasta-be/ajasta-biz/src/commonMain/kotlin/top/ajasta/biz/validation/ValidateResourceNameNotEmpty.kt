package top.ajasta.biz.validation

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext

fun ICorChainDsl<BizContext>.validateResourceNameNotEmpty(title: String) = worker {
    this.title = title
    this.description = "Validates that resource name is not empty"
    on { resourceValidating.name.isEmpty() }
    handle {
        addError(
            code = "validation-name-empty",
            group = "validation",
            field = "name",
            message = "Name must not be empty"
        )
    }
}
