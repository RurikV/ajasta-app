package top.ajasta.biz.stubs

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaStubs
import top.ajasta.stubs.AjastaResourceStubs

fun ICorChainDsl<BizContext>.stubResourceUpdateSuccess(title: String) = worker {
    this.title = title
    this.description = "Success case for resource update"
    on { stubCase == AjastaStubs.SUCCESS && state == AjastaState.RUNNING }
    handle {
        state = AjastaState.FINISHING
        val stub = AjastaResourceStubs.RESOURCE_TENNIS_COURT.copy(
            name = resourceRequest.name.takeIf { it.isNotBlank() } ?: AjastaResourceStubs.RESOURCE_TENNIS_COURT.name,
            description = resourceRequest.description.takeIf { it.isNotBlank() } ?: AjastaResourceStubs.RESOURCE_TENNIS_COURT.description
        )
        resourceResponse = stub
    }
}
