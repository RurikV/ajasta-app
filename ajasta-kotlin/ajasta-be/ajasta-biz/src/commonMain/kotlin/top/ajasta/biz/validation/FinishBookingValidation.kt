package top.ajasta.biz.validation

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState

fun ICorChainDsl<BizContext>.finishBookingValidation(title: String) = worker {
    this.title = title
    on { state == AjastaState.RUNNING }
    handle {
        bookingValidated = bookingValidating
    }
}
