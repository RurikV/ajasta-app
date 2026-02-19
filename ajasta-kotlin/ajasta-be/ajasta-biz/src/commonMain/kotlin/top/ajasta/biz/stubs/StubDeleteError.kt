package top.ajasta.biz.stubs

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaStubs

fun ICorChainDsl<BizContext>.stubDeleteError(title: String) = worker {
    this.title = title
    this.description = "Error case for delete error"
    on { stubCase == AjastaStubs.DELETE_ERROR && state == AjastaState.RUNNING }
    handle {
        state = AjastaState.FAILING
        addError(
            code = "delete-error",
            group = "business",
            field = "id",
            message = "Cannot delete booking with active payment"
        )
    }
}
