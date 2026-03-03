package top.ajasta.biz.repo

import top.ajasta.biz.BizContext
import top.ajasta.common.models.AjastaResourceType
import top.ajasta.common.models.AjastaState
import top.ajasta.common.models.AjastaUserId
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
    description = "Preparing resource for update by merging with existing data"
    on { state == AjastaState.RUNNING }
    handle {
        // Start with existing data from DB to preserve fields not sent in request
        resourceRepoPrepare = resourceRepoRead.deepCopy()

        // Override with non-default values from the validated request
        resourceRepoPrepare = resourceRepoPrepare.copy(
            // Always use request values for core fields (these are typically always sent)
            id = resourceValidated.id,
            name = resourceValidated.name.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.name,
            type = resourceValidated.type.takeIf { it != AjastaResourceType.NONE } ?: resourceRepoPrepare.type,
            location = resourceValidated.location.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.location,
            description = resourceValidated.description.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.description,
            imageUrl = resourceValidated.imageUrl.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.imageUrl,
            pricePerSlot = resourceValidated.pricePerSlot.takeIf { it != 0.0 } ?: resourceRepoPrepare.pricePerSlot,
            unitsCount = resourceValidated.unitsCount.takeIf { it != 1 } ?: resourceRepoPrepare.unitsCount,
            openTime = resourceValidated.openTime.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.openTime,
            closeTime = resourceValidated.closeTime.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.closeTime,
            ownerId = resourceValidated.ownerId.takeIf { it != AjastaUserId.NONE } ?: resourceRepoPrepare.ownerId,
            // Availability fields - preserve existing if not explicitly changed
            active = resourceValidated.active,
            unavailableWeekdays = resourceValidated.unavailableWeekdays.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.unavailableWeekdays,
            unavailableDates = resourceValidated.unavailableDates.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.unavailableDates,
            dailyUnavailableRanges = resourceValidated.dailyUnavailableRanges.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.dailyUnavailableRanges,
            // Preserve existing lock from DB (will be updated after successful update)
            lock = resourceRepoRead.lock
        )
    }
}
