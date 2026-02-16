package top.ajasta.stubs

import top.ajasta.common.models.*

object AjastaBookingStub {
    fun get(): AjastaBooking = AjastaBookingStubs.BOOKING_TENNIS.copy()

    fun prepareResult(block: AjastaBooking.() -> Unit): AjastaBooking = get().apply(block)

    fun prepareSearchList(filterResourceId: String = "") = listOf(
        booking("booking-101", filterResourceId),
        booking("booking-102", filterResourceId),
        booking("booking-103", filterResourceId),
    )

    private fun booking(id: String, filterResourceId: String) =
        AjastaBookingStubs.BOOKING_TENNIS.copy(
            id = AjastaBookingId(id),
            resourceId = AjastaResourceId(filterResourceId.ifEmpty { "resource-tennis-001" })
        )
}

object AjastaResourceStub {
    fun get(): AjastaResource = AjastaResourceStubs.RESOURCE_TENNIS_COURT.copy()

    fun prepareResult(block: AjastaResource.() -> Unit): AjastaResource = get().apply(block)

    fun prepareSearchList(filterType: AjastaResourceType = AjastaResourceType.NONE) = listOf(
        AjastaResourceStubs.RESOURCE_TENNIS_COURT,
        AjastaResourceStubs.RESOURCE_VOLLEYBALL_COURT,
        AjastaResourceStubs.RESOURCE_HAIRDRESSING,
        AjastaResourceStubs.RESOURCE_PLAYGROUND
    ).filter {
        if (filterType == AjastaResourceType.NONE) true else it.type == filterType
    }

    fun prepareAvailabilityList(): List<AjastaSlot> = listOf(
        AjastaSlot(
            slotStart = kotlinx.datetime.Instant.parse("2025-03-01T10:00:00Z"),
            slotEnd = kotlinx.datetime.Instant.parse("2025-03-01T11:00:00Z"),
            price = 30.0
        ),
        AjastaSlot(
            slotStart = kotlinx.datetime.Instant.parse("2025-03-01T11:00:00Z"),
            slotEnd = kotlinx.datetime.Instant.parse("2025-03-01T12:00:00Z"),
            price = 30.0
        ),
        AjastaSlot(
            slotStart = kotlinx.datetime.Instant.parse("2025-03-01T12:00:00Z"),
            slotEnd = kotlinx.datetime.Instant.parse("2025-03-01T13:00:00Z"),
            price = 30.0
        )
    )
}
