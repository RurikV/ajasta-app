package top.ajasta.common.models

import kotlinx.datetime.Instant

/**
 * Filter criteria for booking search.
 */
data class AjastaBookingFilter(
    var resourceId: AjastaResourceId = AjastaResourceId.NONE,
    var userId: AjastaUserId = AjastaUserId.NONE,
    var status: AjastaBookingStatus = AjastaBookingStatus.NONE,
    var dateFrom: Instant = Instant.DISTANT_PAST,
    var dateTo: Instant = Instant.DISTANT_PAST
) {
    fun deepCopy(): AjastaBookingFilter = copy()

    fun isEmpty() = this == NONE

    companion object {
        val NONE = AjastaBookingFilter()
    }
}
