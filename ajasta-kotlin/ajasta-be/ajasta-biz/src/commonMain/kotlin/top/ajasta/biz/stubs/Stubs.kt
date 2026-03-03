package top.ajasta.biz.stubs

import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaWorkMode
import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.chain

fun ICorChainDsl<BizContext>.stubs(title: String, block: ICorChainDsl<BizContext>.() -> Unit) = chain {
    block()
    this.title = title
    on { workMode == AjastaWorkMode.STUB && state == AjastaState.RUNNING }
}
