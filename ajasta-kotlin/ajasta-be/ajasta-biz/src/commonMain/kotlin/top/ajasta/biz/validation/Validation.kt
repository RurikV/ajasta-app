package top.ajasta.biz.validation

import top.ajasta.common.AjastaContext
import top.ajasta.common.models.AjastaState
import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.chain

fun ICorChainDsl<AjastaContext>.validation(block: ICorChainDsl<AjastaContext>.() -> Unit) = chain {
    block()
    title = "Validation"
    on { state == AjastaState.RUNNING }
}
