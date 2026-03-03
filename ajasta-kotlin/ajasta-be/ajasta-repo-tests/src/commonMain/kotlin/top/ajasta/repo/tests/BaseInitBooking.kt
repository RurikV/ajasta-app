package top.ajasta.repo.tests

import top.ajasta.common.models.*

abstract class BaseInitBooking(private val op: String) : IInitObjects<AjastaBooking> {
    open val lockOld: AjastaLock = AjastaLock("20000000-0000-0000-0000-000000000001")
    open val lockBad: AjastaLock = AjastaLock("20000000-0000-0000-0000-000000000009")

    fun createInitTestModel(
        suf: String,
        resourceId: AjastaResourceId = AjastaResourceId("resource-123"),
        userId: AjastaUserId = AjastaUserId("user-123"),
        lock: AjastaLock = lockOld,
        status: AjastaBookingStatus = AjastaBookingStatus.PENDING
    ) = AjastaBooking(
        id = AjastaBookingId("booking-repo-$op-$suf"),
        resourceId = resourceId,
        userId = userId,
        title = "$suf booking",
        description = "$suf booking description",
        totalAmount = 100.0,
        bookingStatus = status,
        lock = lock
    )
}

internal interface IInitObjects<T> {
    val initObjects: Collection<T>
}
