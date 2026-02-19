package top.ajasta.biz.stubs

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaBookingStatus
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaStubs
import top.ajasta.stubs.AjastaBookingStubs

fun ICorChainDsl<BizContext>.stubBookingDeleteSuccess(title: String) = worker {
    this.title = title
    this.description = "Success case for booking delete"
    on { stubCase == AjastaStubs.SUCCESS && state == AjastaState.RUNNING }
    handle {
        state = AjastaState.FINISHING
        bookingResponse = AjastaBookingStubs.BOOKING_TENNIS.copy(
            bookingStatus = AjastaBookingStatus.CANCELLED
        )
        refundAmount = 50.0
    }
}
