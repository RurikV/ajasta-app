package top.ajasta.stubs

import kotlinx.datetime.Instant
import top.ajasta.common.models.*

object AjastaBookingStubs {
    val BOOKING_TENNIS: AjastaBooking
        get() = AjastaBooking(
            id = AjastaBookingId("booking-001"),
            resourceId = AjastaResourceId("resource-tennis-001"),
            userId = AjastaUserId("user-001"),
            title = "Tennis Court Morning Session",
            description = "Weekly tennis practice",
            slots = listOf(
                AjastaSlot(
                    slotStart = Instant.parse("2025-03-01T10:00:00Z"),
                    slotEnd = Instant.parse("2025-03-01T11:00:00Z"),
                    price = 30.0
                )
            ),
            totalAmount = 30.0,
            bookingStatus = AjastaBookingStatus.CONFIRMED,
            paymentStatus = AjastaPaymentStatus.COMPLETED,
            lock = AjastaLock("lock-001")
        )

    val BOOKING_HAIRDRESSING: AjastaBooking
        get() = AjastaBooking(
            id = AjastaBookingId("booking-002"),
            resourceId = AjastaResourceId("resource-hair-001"),
            userId = AjastaUserId("user-002"),
            title = "Haircut Appointment",
            description = "Monthly haircut",
            slots = listOf(
                AjastaSlot(
                    slotStart = Instant.parse("2025-03-02T14:00:00Z"),
                    slotEnd = Instant.parse("2025-03-02T14:30:00Z"),
                    price = 25.0
                )
            ),
            totalAmount = 25.0,
            bookingStatus = AjastaBookingStatus.PENDING,
            paymentStatus = AjastaPaymentStatus.PENDING,
            lock = AjastaLock("lock-002")
        )
}
