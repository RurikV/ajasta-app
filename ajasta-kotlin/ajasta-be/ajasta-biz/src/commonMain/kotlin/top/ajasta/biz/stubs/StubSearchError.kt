package top.ajasta.biz.stubs

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaStubs

fun ICorChainDsl<BizContext>.stubSearchError(title: String) = worker {
    this.title = title
    this.description = "Error case for search error"
    on { stubCase == AjastaStubs.SEARCH_ERROR && state == AjastaState.RUNNING }
    handle {
        state = AjastaState.FAILING
        addError(
            code = "search-error",
            group = "business",
            field = "filter",
            message = "Search operation failed"
        )
    }
}
