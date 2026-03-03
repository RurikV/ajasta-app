package top.ajasta.biz.stubs

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaStubs

fun ICorChainDsl<BizContext>.stubNotFoundError(title: String) = worker {
    this.title = title
    this.description = "Error case for not found"
    on { stubCase == AjastaStubs.NOT_FOUND && state == AjastaState.RUNNING }
    handle {
        state = AjastaState.FAILING
        addError(
            code = "not-found",
            group = "business",
            field = "id",
            message = "Resource or booking not found"
        )
    }
}
