package top.ajasta.biz.stubs

import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaSlot
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaStubs
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

fun ICorChainDsl<BizContext>.stubGetAvailabilitySuccess(title: String) = worker {
    this.title = title
    this.description = "Success case for get availability"
    on { stubCase == AjastaStubs.SUCCESS && state == AjastaState.RUNNING }
    handle {
        state = AjastaState.FINISHING
        // Generate some mock available slots
        val now = Clock.System.now()
        availableSlots.clear()
        for (i in 0..5) {
            availableSlots.add(
                AjastaSlot(
                    slotStart = now + i.hours,
                    slotEnd = now + (i + 1).hours,
                    price = 25.0 + (i * 5)
                )
            )
        }
    }
}
