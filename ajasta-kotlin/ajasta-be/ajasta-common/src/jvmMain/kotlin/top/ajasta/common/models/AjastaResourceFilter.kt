package top.ajasta.common.models

import kotlinx.datetime.Instant

/**
 * Filter criteria for resource search.
 */
data class AjastaResourceFilter(
    var type: AjastaResourceType = AjastaResourceType.NONE,
    var location: String = "",
    var minPrice: Double = 0.0,
    var maxPrice: Double = 0.0,
    var minRating: Double = 0.0,
    var ownerId: AjastaUserId = AjastaUserId.NONE
) {
    fun deepCopy(): AjastaResourceFilter = copy()

    fun isEmpty() = this == NONE

    companion object {
        val NONE = AjastaResourceFilter()
    }
}
