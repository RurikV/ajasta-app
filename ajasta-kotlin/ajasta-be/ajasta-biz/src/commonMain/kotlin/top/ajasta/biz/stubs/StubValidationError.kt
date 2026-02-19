package top.ajasta.biz.stubs

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaStubs

fun ICorChainDsl<BizContext>.stubValidationError(title: String) = worker {
    this.title = title
    this.description = "Error case for validation error"
    on { stubCase == AjastaStubs.VALIDATION_ERROR && state == AjastaState.RUNNING }
    handle {
        state = AjastaState.FAILING
        addError(
            code = "validation-error",
            group = "validation",
            field = "title",
            message = "Title is too short"
        )
    }
}
