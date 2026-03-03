package top.ajasta.biz.validation

import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState
import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.chain

fun ICorChainDsl<BizContext>.validation(block: ICorChainDsl<BizContext>.() -> Unit) = chain {
    block()
    title = "Validation"
    on { state == AjastaState.RUNNING }
}
