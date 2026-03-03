package top.ajasta.biz.validation

import top.ajasta.biz.BizContext
import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker

fun ICorChainDsl<BizContext>.validateAvailabilityDateRange(title: String) = worker {
    this.title = title
    this.description = "Validates that date range is valid for availability check"
    on {
        availabilityDateFrom != kotlinx.datetime.Instant.DISTANT_PAST &&
        availabilityDateTo != kotlinx.datetime.Instant.DISTANT_PAST &&
        availabilityDateFrom >= availabilityDateTo
    }
    handle {
        addError(
            code = "validation-date-range-invalid",
            group = "validation",
            field = "dateFrom/dateTo",
            message = "Date 'from' must be before date 'to'"
        )
    }
}
