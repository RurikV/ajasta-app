package top.ajasta.biz.validation

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext

fun ICorChainDsl<BizContext>.validateResourcePricePositive(title: String) = worker {
    this.title = title
    this.description = "Validates that resource price is positive"
    on { resourceValidating.pricePerSlot <= 0 }
    handle {
        addError(
            code = "validation-price-negative",
            group = "validation",
            field = "pricePerSlot",
            message = "Price per slot must be positive"
        )
    }
}
