package top.ajasta.biz.stubs

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaStubs
import top.ajasta.stubs.AjastaBookingStubs

fun ICorChainDsl<BizContext>.stubBookingSearchSuccess(title: String) = worker {
    this.title = title
    this.description = "Success case for booking search"
    on { stubCase == AjastaStubs.SUCCESS && state == AjastaState.RUNNING }
    handle {
        state = AjastaState.FINISHING
        val filter = bookingFilterRequest

        // Clear the response list
        bookingsResponse.clear()

        // Filter by resourceId if provided
        if (filter.resourceId != top.ajasta.common.models.AjastaResourceId.NONE) {
            if (AjastaBookingStubs.BOOKING_TENNIS.resourceId == filter.resourceId) {
                bookingsResponse.add(AjastaBookingStubs.BOOKING_TENNIS)
            }
            if (AjastaBookingStubs.BOOKING_HAIRDRESSING.resourceId == filter.resourceId) {
                bookingsResponse.add(AjastaBookingStubs.BOOKING_HAIRDRESSING)
            }
        } else {
            bookingsResponse.add(AjastaBookingStubs.BOOKING_TENNIS)
            bookingsResponse.add(AjastaBookingStubs.BOOKING_HAIRDRESSING)
        }
    }
}
