package top.ajasta.common.models

import kotlinx.datetime.Instant

/**
 * Represents a single time slot in a booking.
 */
data class AjastaSlot(
    var slotStart: Instant = Instant.DISTANT_PAST,
    var slotEnd: Instant = Instant.DISTANT_PAST,
    var price: Double = 0.0
) {
    fun deepCopy(): AjastaSlot = copy()

    fun isEmpty() = this == NONE

    companion object {
        val NONE = AjastaSlot()
    }
}
