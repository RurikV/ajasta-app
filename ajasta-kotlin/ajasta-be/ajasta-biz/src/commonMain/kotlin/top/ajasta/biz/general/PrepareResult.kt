package top.ajasta.biz.general

import top.ajasta.common.AjastaContext
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaWorkMode
import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker

fun ICorChainDsl<AjastaContext>.prepareResult(title: String) = worker {
    this.title = title
    description = "Preparing data for client response"
    on { workMode != AjastaWorkMode.STUB }
    handle {
        bookingResponse = bookingRepoDone
        bookingsResponse = bookingsRepoDone
        resourceResponse = resourceRepoDone
        resourcesResponse = resourcesRepoDone
        state = when (val st = state) {
            AjastaState.RUNNING -> AjastaState.FINISHING
            else -> st
        }
    }
}
