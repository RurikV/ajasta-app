package top.ajasta.biz.stubs

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState

fun ICorChainDsl<BizContext>.stubNoCase(title: String) = worker {
    this.title = title
    this.description = "Validates situation when requested case is not supported in stubs"
    on { state == AjastaState.RUNNING }
    handle {
        addError(
            code = "validation",
            field = "stub",
            group = "validation",
            message = "Wrong stub case is requested: ${stubCase.name}"
        )
    }
}
