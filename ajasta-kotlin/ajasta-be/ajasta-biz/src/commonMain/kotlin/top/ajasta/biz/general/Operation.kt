package top.ajasta.biz.general

import top.ajasta.common.AjastaContext
import top.ajasta.common.models.AjastaCommand
import top.ajasta.common.models.AjastaState
import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.chain

fun ICorChainDsl<AjastaContext>.operation(
    title: String,
    command: AjastaCommand,
    block: ICorChainDsl<AjastaContext>.() -> Unit
) = chain {
    block()
    this.title = title
    on { this.command == command && state == AjastaState.RUNNING }
}
