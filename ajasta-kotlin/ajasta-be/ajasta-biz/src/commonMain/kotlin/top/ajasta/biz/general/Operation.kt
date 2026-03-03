package top.ajasta.biz.general

import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaCommand
import top.ajasta.common.models.AjastaState
import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.chain

fun ICorChainDsl<BizContext>.operation(
    title: String,
    command: AjastaCommand,
    block: ICorChainDsl<BizContext>.() -> Unit
) = chain {
    block()
    this.title = title
    on { this.command == command && state == AjastaState.RUNNING }
}
