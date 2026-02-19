package top.ajasta.biz.stubs

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaStubs
import top.ajasta.stubs.AjastaBookingStubs

fun ICorChainDsl<BizContext>.stubBookingUpdateSuccess(title: String) = worker {
    this.title = title
    this.description = "Success case for booking update"
    on { stubCase == AjastaStubs.SUCCESS && state == AjastaState.RUNNING }
    handle {
        state = AjastaState.FINISHING
        val stub = AjastaBookingStubs.BOOKING_TENNIS.copy(
            title = bookingRequest.title.takeIf { it.isNotBlank() } ?: AjastaBookingStubs.BOOKING_TENNIS.title,
            description = bookingRequest.description.takeIf { it.isNotBlank() } ?: AjastaBookingStubs.BOOKING_TENNIS.description
        )
        bookingResponse = stub
    }
}
