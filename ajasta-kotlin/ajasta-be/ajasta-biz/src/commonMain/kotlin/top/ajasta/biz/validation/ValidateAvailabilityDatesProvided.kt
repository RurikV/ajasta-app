package top.ajasta.biz.validation

import top.ajasta.biz.BizContext
import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker

fun ICorChainDsl<BizContext>.validateAvailabilityDatesProvided(title: String) = worker {
    this.title = title
    this.description = "Validates that dates are provided for availability check"
    on {
        availabilityDateFrom == kotlinx.datetime.Instant.DISTANT_PAST ||
        availabilityDateTo == kotlinx.datetime.Instant.DISTANT_PAST
    }
    handle {
        addError(
            code = "validation-dates-not-provided",
            group = "validation",
            field = "dateFrom/dateTo",
            message = "Both dateFrom and dateTo must be provided for availability check"
        )
    }
}
