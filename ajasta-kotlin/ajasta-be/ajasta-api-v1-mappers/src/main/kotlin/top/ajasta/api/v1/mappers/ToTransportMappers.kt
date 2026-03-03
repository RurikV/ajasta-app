package top.ajasta.api.v1.mappers

import top.ajasta.api.v1.models.*
import top.ajasta.common.AjastaContext
import top.ajasta.common.models.*

/**
 * Extension function to map context to transport response.
 * Entry point for all response mapping.
 * Returns IResponse interface for proper POSTful polymorphic handling.
 */
fun AjastaContext.toTransport(): IResponse = when (command) {
    AjastaCommand.CREATE_BOOKING -> toTransportCreateBooking()
    AjastaCommand.READ_BOOKING -> toTransportReadBooking()
    AjastaCommand.UPDATE_BOOKING -> toTransportUpdateBooking()
    AjastaCommand.DELETE_BOOKING -> toTransportDeleteBooking()
    AjastaCommand.SEARCH_BOOKINGS -> toTransportSearchBookings()
    AjastaCommand.CREATE_RESOURCE -> toTransportCreateResource()
    AjastaCommand.READ_RESOURCE -> toTransportReadResource()
    AjastaCommand.UPDATE_RESOURCE -> toTransportUpdateResource()
    AjastaCommand.DELETE_RESOURCE -> toTransportDeleteResource()
    AjastaCommand.SEARCH_RESOURCES -> toTransportSearchResources()
    AjastaCommand.GET_AVAILABILITY -> toTransportAvailability()
    AjastaCommand.NONE -> throw IllegalStateException("No command set")
}

// === Booking Response Mappers ===

fun AjastaContext.toTransportCreateBooking() = BookingCreateResponse(
    responseType = "createBooking",
    requestId = requestId.asString().takeIf { it.isNotEmpty() },
    responseTime = kotlinx.datetime.Clock.System.now().toString(),
    booking = bookingResponse.takeIf { !it.isEmpty() }?.toTransport(),
    paymentLink = paymentLink.takeIf { it.isNotEmpty() },
    errors = errors.toTransportErrors()
)

fun AjastaContext.toTransportReadBooking() = BookingReadResponse(
    responseType = "readBooking",
    requestId = requestId.asString().takeIf { it.isNotEmpty() },
    responseTime = kotlinx.datetime.Clock.System.now().toString(),
    booking = bookingResponse.takeIf { !it.isEmpty() }?.toTransport(),
    errors = errors.toTransportErrors()
)

fun AjastaContext.toTransportUpdateBooking() = BookingUpdateResponse(
    responseType = "updateBooking",
    requestId = requestId.asString().takeIf { it.isNotEmpty() },
    responseTime = kotlinx.datetime.Clock.System.now().toString(),
    booking = bookingResponse.takeIf { !it.isEmpty() }?.toTransport(),
    errors = errors.toTransportErrors()
)

fun AjastaContext.toTransportDeleteBooking() = BookingDeleteResponse(
    responseType = "deleteBooking",
    requestId = requestId.asString().takeIf { it.isNotEmpty() },
    responseTime = kotlinx.datetime.Clock.System.now().toString(),
    booking = bookingResponse.takeIf { !it.isEmpty() }?.toTransport(),
    errors = errors.toTransportErrors()
)

fun AjastaContext.toTransportSearchBookings() = BookingSearchResponse(
    responseType = "searchBookings",
    requestId = requestId.asString().takeIf { it.isNotEmpty() },
    responseTime = kotlinx.datetime.Clock.System.now().toString(),
    bookings = bookingsResponse.map { it.toTransport() }.takeIf { it.isNotEmpty() },
    errors = errors.toTransportErrors()
)

// === Resource Response Mappers ===

fun AjastaContext.toTransportCreateResource() = ResourceCreateResponse(
    responseType = "createResource",
    requestId = requestId.asString().takeIf { it.isNotEmpty() },
    responseTime = kotlinx.datetime.Clock.System.now().toString(),
    resource = resourceResponse.takeIf { !it.isEmpty() }?.toTransport(),
    errors = errors.toTransportErrors()
)

fun AjastaContext.toTransportReadResource() = ResourceReadResponse(
    responseType = "readResource",
    requestId = requestId.asString().takeIf { it.isNotEmpty() },
    responseTime = kotlinx.datetime.Clock.System.now().toString(),
    resource = resourceResponse.takeIf { !it.isEmpty() }?.toTransport(),
    errors = errors.toTransportErrors()
)

fun AjastaContext.toTransportUpdateResource() = ResourceUpdateResponse(
    responseType = "updateResource",
    requestId = requestId.asString().takeIf { it.isNotEmpty() },
    responseTime = kotlinx.datetime.Clock.System.now().toString(),
    resource = resourceResponse.takeIf { !it.isEmpty() }?.toTransport(),
    errors = errors.toTransportErrors()
)

fun AjastaContext.toTransportDeleteResource() = ResourceDeleteResponse(
    responseType = "deleteResource",
    requestId = requestId.asString().takeIf { it.isNotEmpty() },
    responseTime = kotlinx.datetime.Clock.System.now().toString(),
    resource = resourceResponse.takeIf { !it.isEmpty() }?.toTransport(),
    errors = errors.toTransportErrors()
)

fun AjastaContext.toTransportSearchResources() = ResourceSearchResponse(
    responseType = "searchResources",
    requestId = requestId.asString().takeIf { it.isNotEmpty() },
    responseTime = kotlinx.datetime.Clock.System.now().toString(),
    resources = resourcesResponse.map { it.toTransport() }.takeIf { it.isNotEmpty() },
    errors = errors.toTransportErrors()
)

// === Availability Response Mapper ===

fun AjastaContext.toTransportAvailability() = AvailabilityResponse(
    responseType = "getAvailability",
    requestId = requestId.asString().takeIf { it.isNotEmpty() },
    responseTime = kotlinx.datetime.Clock.System.now().toString(),
    resourceId = availabilityResourceId.asString().takeIf { it.isNotEmpty() },
    slots = availableSlots.map { it.toTransportAvailability() }.takeIf { it.isNotEmpty() },
    errors = errors.toTransportErrors()
)

// === Object Mappers ===

fun AjastaBooking.toTransport() = BookingObject(
    id = id.asString().takeIf { it.isNotEmpty() },
    resourceId = resourceId.asString().takeIf { it.isNotEmpty() },
    userId = userId.asString().takeIf { it.isNotEmpty() },
    title = title.takeIf { it.isNotEmpty() },
    description = description.takeIf { it.isNotEmpty() },
    slots = slots.map { it.toTransport() }.takeIf { it.isNotEmpty() },
    totalAmount = totalAmount.takeIf { it > 0 },
    bookingStatus = bookingStatus.toTransport(),
    paymentStatus = paymentStatus.toTransport(),
    lock = lock.asString().takeIf { it.isNotEmpty() },
    createdAt = createdAt.takeIf { it != kotlinx.datetime.Instant.DISTANT_PAST }?.toString(),
    updatedAt = updatedAt.takeIf { it != kotlinx.datetime.Instant.DISTANT_PAST }?.toString()
)

fun AjastaSlot.toTransport() = BookingSlot(
    slotStart = slotStart.takeIf { it != kotlinx.datetime.Instant.DISTANT_PAST }?.toString(),
    slotEnd = slotEnd.takeIf { it != kotlinx.datetime.Instant.DISTANT_PAST }?.toString(),
    price = price.takeIf { it > 0 }
)

fun AjastaSlot.toTransportAvailability() = AvailabilitySlot(
    slotStart = slotStart.takeIf { it != kotlinx.datetime.Instant.DISTANT_PAST }?.toString(),
    slotEnd = slotEnd.takeIf { it != kotlinx.datetime.Instant.DISTANT_PAST }?.toString(),
    available = true,
    availableUnits = 1,
    price = price.takeIf { it > 0 }
)

fun AjastaResource.toTransport() = ResourceObject(
    id = id.asString().takeIf { it.isNotEmpty() },
    name = name.takeIf { it.isNotEmpty() },
    type = type.toTransport(),
    location = location.takeIf { it.isNotEmpty() },
    description = description.takeIf { it.isNotEmpty() },
    imageUrl = imageUrl.takeIf { it.isNotEmpty() },
    pricePerSlot = pricePerSlot.takeIf { it > 0 },
    unitsCount = unitsCount.takeIf { it > 0 },
    openTime = openTime.takeIf { it.isNotEmpty() },
    closeTime = closeTime.takeIf { it.isNotEmpty() },
    rating = rating.takeIf { it > 0 },
    reviewCount = reviewCount.takeIf { it > 0 },
    ownerId = ownerId.asString().takeIf { it.isNotEmpty() },
    active = active,
    unavailableWeekdays = unavailableWeekdays.takeIf { it.isNotEmpty() },
    unavailableDates = unavailableDates.takeIf { it.isNotEmpty() },
    dailyUnavailableRanges = dailyUnavailableRanges.takeIf { it.isNotEmpty() },
    lock = lock.asString().takeIf { it.isNotEmpty() },
    createdAt = createdAt.takeIf { it != kotlinx.datetime.Instant.DISTANT_PAST }?.toString(),
    updatedAt = updatedAt.takeIf { it != kotlinx.datetime.Instant.DISTANT_PAST }?.toString()
)

// === Enum Mappers ===

fun AjastaBookingStatus.toTransport() = when (this) {
    AjastaBookingStatus.PENDING -> BookingStatus.PENDING
    AjastaBookingStatus.CONFIRMED -> BookingStatus.CONFIRMED
    AjastaBookingStatus.CANCELLED -> BookingStatus.CANCELLED
    AjastaBookingStatus.COMPLETED -> BookingStatus.COMPLETED
    AjastaBookingStatus.NONE -> null
}

fun AjastaPaymentStatus.toTransport() = when (this) {
    AjastaPaymentStatus.PENDING -> PaymentStatus.PENDING
    AjastaPaymentStatus.COMPLETED -> PaymentStatus.COMPLETED
    AjastaPaymentStatus.FAILED -> PaymentStatus.FAILED
    AjastaPaymentStatus.REFUNDED -> PaymentStatus.REFUNDED
    AjastaPaymentStatus.NONE -> null
}

fun AjastaResourceType.toTransport() = when (this) {
    AjastaResourceType.TURF_COURT -> ResourceType.TURF_COURT
    AjastaResourceType.VOLLEYBALL_COURT -> ResourceType.VOLLEYBALL_COURT
    AjastaResourceType.PLAYGROUND -> ResourceType.PLAYGROUND
    AjastaResourceType.HAIRDRESSING_CHAIR -> ResourceType.HAIRDRESSING_CHAIR
    AjastaResourceType.OTHER -> ResourceType.OTHER
    AjastaResourceType.NONE -> null
}

// === Error Mapper ===

fun List<AjastaError>.toTransportErrors(): List<Error>? = this
    .map { it.toTransport() }
    .takeIf { it.isNotEmpty() }

fun AjastaError.toTransport() = Error(
    code = code.takeIf { it.isNotEmpty() },
    group = group.takeIf { it.isNotEmpty() },
    field = field.takeIf { it.isNotEmpty() },
    message = message.takeIf { it.isNotEmpty() }
)
