package top.ajasta.biz.stubs

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaResourceType
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaStubs
import top.ajasta.stubs.AjastaResourceStubs

fun ICorChainDsl<BizContext>.stubResourceSearchSuccess(title: String) = worker {
    this.title = title
    this.description = "Success case for resource search"
    on { stubCase == AjastaStubs.SUCCESS && state == AjastaState.RUNNING }
    handle {
        state = AjastaState.FINISHING
        val filter = resourceFilterRequest

        // Add resources based on type filter
        if (filter.type == AjastaResourceType.NONE || filter.type == AjastaResourceType.TURF_COURT) {
            resourcesResponse.add(AjastaResourceStubs.RESOURCE_TENNIS_COURT)
        }
        if (filter.type == AjastaResourceType.NONE || filter.type == AjastaResourceType.VOLLEYBALL_COURT) {
            resourcesResponse.add(AjastaResourceStubs.RESOURCE_VOLLEYBALL_COURT)
        }
        if (filter.type == AjastaResourceType.NONE || filter.type == AjastaResourceType.HAIRDRESSING_CHAIR) {
            resourcesResponse.add(AjastaResourceStubs.RESOURCE_HAIRDRESSING)
        }
        if (filter.type == AjastaResourceType.NONE || filter.type == AjastaResourceType.PLAYGROUND) {
            resourcesResponse.add(AjastaResourceStubs.RESOURCE_PLAYGROUND)
        }
    }
}
