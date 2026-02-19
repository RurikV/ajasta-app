package top.ajasta.biz.general

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState

fun ICorChainDsl<BizContext>.initStatus(title: String) = worker {
    this.title = title
    this.description = """
        This handler sets the initial processing status.
        Runs only when status is not set.
    """.trimIndent()
    on { state == AjastaState.NONE }
    handle { state = AjastaState.RUNNING }
}
