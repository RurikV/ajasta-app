package top.ajasta.biz.general

import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaWorkMode
import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker

fun ICorChainDsl<BizContext>.prepareResult(title: String) = worker {
    this.title = title
    description = "Preparing data for client response"
    on { workMode != AjastaWorkMode.STUB }
    handle {
        // For single entity operations, check both repoDone and repoRead
        if (bookingRepoDone.isEmpty()) {
            bookingResponse = bookingRepoRead
        } else {
            bookingResponse = bookingRepoDone
        }

        bookingsResponse.clear()
        bookingsResponse.addAll(bookingsRepoDone)

        // For single entity operations, check both repoDone and repoRead
        if (resourceRepoDone.isEmpty()) {
            resourceResponse = resourceRepoRead
        } else {
            resourceResponse = resourceRepoDone
        }

        resourcesResponse.clear()
        resourcesResponse.addAll(resourcesRepoDone)

        state = when (val st = state) {
            AjastaState.RUNNING -> AjastaState.FINISHING
            else -> st
        }
    }
}
