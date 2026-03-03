package top.ajasta.common.models

import kotlinx.datetime.Instant

/**
 * Internal model representing a bookable resource.
 * Resources are the entities that can be booked (courts, chairs, etc.)
 */
data class AjastaResource(
    var id: AjastaResourceId = AjastaResourceId.NONE,
    var name: String = "",
    var type: AjastaResourceType = AjastaResourceType.NONE,
    var location: String = "",
    var description: String = "",
    var imageUrl: String = "",
    var pricePerSlot: Double = 0.0,
    var unitsCount: Int = 1,
    var openTime: String = "",
    var closeTime: String = "",
    var rating: Double = 0.0,
    var reviewCount: Int = 0,
    var ownerId: AjastaUserId = AjastaUserId.NONE,
    var active: Boolean = true,
    var unavailableWeekdays: String = "",
    var unavailableDates: String = "",
    var dailyUnavailableRanges: String = "",
    var lock: AjastaLock = AjastaLock.NONE,
    var createdAt: Instant = Instant.DISTANT_PAST,
    var updatedAt: Instant = Instant.DISTANT_PAST
) {
    fun deepCopy(): AjastaResource = copy()

    fun isEmpty() = this == NONE

    companion object {
        val NONE = AjastaResource()
    }
}
