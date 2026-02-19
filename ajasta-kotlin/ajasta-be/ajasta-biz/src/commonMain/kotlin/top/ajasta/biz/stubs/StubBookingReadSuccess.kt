package top.ajasta.biz.stubs

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaStubs
import top.ajasta.stubs.AjastaBookingStubs

fun ICorChainDsl<BizContext>.stubBookingReadSuccess(title: String) = worker {
    this.title = title
    this.description = "Success case for booking read"
    on { stubCase == AjastaStubs.SUCCESS && state == AjastaState.RUNNING }
    handle {
        state = AjastaState.FINISHING
        bookingResponse = AjastaBookingStubs.BOOKING_TENNIS
    }
}
