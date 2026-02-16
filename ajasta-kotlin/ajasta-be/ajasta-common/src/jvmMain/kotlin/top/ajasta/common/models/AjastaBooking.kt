package top.ajasta.common.models

import kotlinx.datetime.Instant

/**
 * Internal model representing a booking.
 * This is the core entity for the booking system.
 */
data class AjastaBooking(
    var id: AjastaBookingId = AjastaBookingId.NONE,
    var resourceId: AjastaResourceId = AjastaResourceId.NONE,
    var userId: AjastaUserId = AjastaUserId.NONE,
    var title: String = "",
    var description: String = "",
    var slots: List<AjastaSlot> = emptyList(),
    var totalAmount: Double = 0.0,
    var bookingStatus: AjastaBookingStatus = AjastaBookingStatus.NONE,
    var paymentStatus: AjastaPaymentStatus = AjastaPaymentStatus.NONE,
    var lock: AjastaLock = AjastaLock.NONE,
    var createdAt: Instant = Instant.DISTANT_PAST,
    var updatedAt: Instant = Instant.DISTANT_PAST
) {
    fun deepCopy(): AjastaBooking = copy(
        slots = slots.map { it.deepCopy() }
    )

    fun isEmpty() = this == NONE

    companion object {
        val NONE = AjastaBooking()
    }
}
