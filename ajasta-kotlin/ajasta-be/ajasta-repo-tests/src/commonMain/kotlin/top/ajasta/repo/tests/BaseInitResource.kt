package top.ajasta.repo.tests

import top.ajasta.common.models.*

abstract class BaseInitResource(private val op: String) : IInitObjects<AjastaResource> {
    open val lockOld: AjastaLock = AjastaLock("20000000-0000-0000-0000-000000000001")
    open val lockBad: AjastaLock = AjastaLock("20000000-0000-0000-0000-000000000009")

    fun createInitTestModel(
        suf: String,
        ownerId: AjastaUserId = AjastaUserId("owner-123"),
        type: AjastaResourceType = AjastaResourceType.TURF_COURT,
        lock: AjastaLock = lockOld,
        pricePerSlot: Double = 100.0,
        active: Boolean = true,
        unavailableWeekdays: String = "",
        unavailableDates: String = "",
        dailyUnavailableRanges: String = ""
    ) = AjastaResource(
        id = AjastaResourceId("resource-repo-$op-$suf"),
        name = "$suf resource",
        type = type,
        location = "Location $suf",
        description = "$suf resource description",
        pricePerSlot = pricePerSlot,
        rating = 4.5,
        ownerId = ownerId,
        active = active,
        unavailableWeekdays = unavailableWeekdays,
        unavailableDates = unavailableDates,
        dailyUnavailableRanges = dailyUnavailableRanges,
        lock = lock
    )
}
