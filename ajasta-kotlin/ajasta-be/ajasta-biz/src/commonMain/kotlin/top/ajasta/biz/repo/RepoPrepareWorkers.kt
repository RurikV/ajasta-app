package top.ajasta.biz.repo

import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaState
import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker

fun ICorChainDsl<BizContext>.repoPrepareBookingCreate(title: String) = worker {
    this.title = title
    description = "Preparing booking for creation"
    on { state == AjastaState.RUNNING }
    handle {
        bookingRepoPrepare = bookingValidated.deepCopy()
    }
}

fun ICorChainDsl<BizContext>.repoPrepareBookingUpdate(title: String) = worker {
    this.title = title
    description = "Preparing booking for update"
    on { state == AjastaState.RUNNING }
    handle {
        bookingRepoPrepare = bookingValidated.deepCopy()
        bookingRepoRead.lock.let { existingLock ->
            if (existingLock.asString().isNotEmpty()) {
                bookingRepoPrepare = bookingRepoPrepare.copy(lock = existingLock)
            }
        }
    }
}

fun ICorChainDsl<BizContext>.repoPrepareResourceCreate(title: String) = worker {
    this.title = title
    description = "Preparing resource for creation"
    on { state == AjastaState.RUNNING }
    handle {
        resourceRepoPrepare = resourceValidated.deepCopy()
    }
}

fun ICorChainDsl<BizContext>.repoPrepareResourceUpdate(title: String) = worker {
    this.title = title
    description = "Preparing resource for update"
    on { state == AjastaState.RUNNING }
    handle {
        resourceRepoPrepare = resourceValidated.deepCopy()
        resourceRepoRead.lock.let { existingLock ->
            if (existingLock.asString().isNotEmpty()) {
                resourceRepoPrepare = resourceRepoPrepare.copy(lock = existingLock)
            }
        }
    }
}
