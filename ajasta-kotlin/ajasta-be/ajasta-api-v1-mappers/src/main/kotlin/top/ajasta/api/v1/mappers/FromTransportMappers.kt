package top.ajasta.api.v1.mappers

import top.ajasta.api.v1.models.*
import top.ajasta.common.AjastaContext
import top.ajasta.common.PaginationDefaults
import top.ajasta.common.models.*

/**
 * Extension function to map transport request to context.
 * Entry point for all request mapping.
 * Uses IRequest interface for proper POSTful polymorphic handling.
 */
fun AjastaContext.fromTransport(request: IRequest): Unit = when (request) {
    is BookingCreateRequest -> fromTransport(request)
    is BookingReadRequest -> fromTransport(request)
    is BookingUpdateRequest -> fromTransport(request)
    is BookingDeleteRequest -> fromTransport(request)
    is BookingSearchRequest -> fromTransport(request)
    is ResourceCreateRequest -> fromTransport(request)
    is ResourceReadRequest -> fromTransport(request)
    is ResourceUpdateRequest -> fromTransport(request)
    is ResourceDeleteRequest -> fromTransport(request)
    is ResourceSearchRequest -> fromTransport(request)
    is AvailabilityRequest -> fromTransport(request)
    // Fallback for any future IRequest implementations
    else -> throw IllegalArgumentException("Unsupported request type: ${request::class}")
}

// === Booking Mappers ===

fun AjastaContext.fromTransport(request: BookingCreateRequest) {
    command = AjastaCommand.CREATE_BOOKING
    requestId = request.requestId?.let { AjastaRequestId(it) } ?: AjastaRequestId.NONE
    bookingRequest = request.booking?.toInternal() ?: AjastaBooking()
    workMode = request.debug.toWorkMode()
    stubCase = request.debug.toStubCase()
}

fun AjastaContext.fromTransport(request: BookingReadRequest) {
    command = AjastaCommand.READ_BOOKING
    requestId = request.requestId?.let { AjastaRequestId(it) } ?: AjastaRequestId.NONE
    bookingRequest = AjastaBooking(
        id = request.booking?.id?.let { AjastaBookingId(it) } ?: AjastaBookingId.NONE
    )
    workMode = request.debug.toWorkMode()
    stubCase = request.debug.toStubCase()
}

fun AjastaContext.fromTransport(request: BookingUpdateRequest) {
    command = AjastaCommand.UPDATE_BOOKING
    requestId = request.requestId?.let { AjastaRequestId(it) } ?: AjastaRequestId.NONE
    bookingRequest = request.booking?.toInternalUpdate() ?: AjastaBooking()
    workMode = request.debug.toWorkMode()
    stubCase = request.debug.toStubCase()
}

fun AjastaContext.fromTransport(request: BookingDeleteRequest) {
    command = AjastaCommand.DELETE_BOOKING
    requestId = request.requestId?.let { AjastaRequestId(it) } ?: AjastaRequestId.NONE
    bookingRequest = AjastaBooking(
        id = request.booking?.id?.let { AjastaBookingId(it) } ?: AjastaBookingId.NONE,
        lock = request.booking?.lock?.let { AjastaLock(it) } ?: AjastaLock.NONE
    )
    workMode = request.debug.toWorkMode()
    stubCase = request.debug.toStubCase()
}

fun AjastaContext.fromTransport(request: BookingSearchRequest) {
    command = AjastaCommand.SEARCH_BOOKINGS
    requestId = request.requestId?.let { AjastaRequestId(it) } ?: AjastaRequestId.NONE
    bookingFilterRequest = request.bookingFilter?.toInternal() ?: AjastaBookingFilter()
    page = request.page ?: PaginationDefaults.DEFAULT_PAGE
    pageSize = request.pageSize ?: PaginationDefaults.DEFAULT_PAGE_SIZE
    workMode = request.debug.toWorkMode()
    stubCase = request.debug.toStubCase()
}

// === Resource Mappers ===

fun AjastaContext.fromTransport(request: ResourceCreateRequest) {
    command = AjastaCommand.CREATE_RESOURCE
    requestId = request.requestId?.let { AjastaRequestId(it) } ?: AjastaRequestId.NONE
    resourceRequest = request.resource?.toInternal() ?: AjastaResource()
    workMode = request.debug.toWorkMode()
    stubCase = request.debug.toStubCase()
}

fun AjastaContext.fromTransport(request: ResourceReadRequest) {
    command = AjastaCommand.READ_RESOURCE
    requestId = request.requestId?.let { AjastaRequestId(it) } ?: AjastaRequestId.NONE
    resourceRequest = AjastaResource(
        id = request.resource?.id?.let { AjastaResourceId(it) } ?: AjastaResourceId.NONE
    )
    workMode = request.debug.toWorkMode()
    stubCase = request.debug.toStubCase()
}

fun AjastaContext.fromTransport(request: ResourceUpdateRequest) {
    command = AjastaCommand.UPDATE_RESOURCE
    requestId = request.requestId?.let { AjastaRequestId(it) } ?: AjastaRequestId.NONE
    resourceRequest = request.resource?.toInternalUpdate() ?: AjastaResource()
    workMode = request.debug.toWorkMode()
    stubCase = request.debug.toStubCase()
}

fun AjastaContext.fromTransport(request: ResourceDeleteRequest) {
    command = AjastaCommand.DELETE_RESOURCE
    requestId = request.requestId?.let { AjastaRequestId(it) } ?: AjastaRequestId.NONE
    resourceRequest = AjastaResource(
        id = request.resource?.id?.let { AjastaResourceId(it) } ?: AjastaResourceId.NONE,
        lock = request.resource?.lock?.let { AjastaLock(it) } ?: AjastaLock.NONE
    )
    workMode = request.debug.toWorkMode()
    stubCase = request.debug.toStubCase()
}

fun AjastaContext.fromTransport(request: ResourceSearchRequest) {
    command = AjastaCommand.SEARCH_RESOURCES
    requestId = request.requestId?.let { AjastaRequestId(it) } ?: AjastaRequestId.NONE
    resourceFilterRequest = request.resourceFilter?.toInternal() ?: AjastaResourceFilter()
    page = request.page ?: PaginationDefaults.DEFAULT_PAGE
    pageSize = request.pageSize ?: PaginationDefaults.DEFAULT_PAGE_SIZE
    workMode = request.debug.toWorkMode()
    stubCase = request.debug.toStubCase()
}

// === Availability Mapper ===

fun AjastaContext.fromTransport(request: AvailabilityRequest) {
    command = AjastaCommand.GET_AVAILABILITY
    requestId = request.requestId?.let { AjastaRequestId(it) } ?: AjastaRequestId.NONE
    availabilityResourceId = request.resourceId?.let { AjastaResourceId(it) } ?: AjastaResourceId.NONE
    availabilityDateFrom = request.dateFrom?.let { kotlinx.datetime.Instant.parse(it) } ?: kotlinx.datetime.Instant.DISTANT_PAST
    availabilityDateTo = request.dateTo?.let { kotlinx.datetime.Instant.parse(it) } ?: kotlinx.datetime.Instant.DISTANT_PAST
    workMode = request.debug.toWorkMode()
    stubCase = request.debug.toStubCase()
}

// === Object Mappers ===

private fun BookingCreateObject.toInternal() = AjastaBooking(
    resourceId = resourceId?.let { AjastaResourceId(it) } ?: AjastaResourceId.NONE,
    title = title ?: "",
    description = description ?: "",
    slots = slots?.map { it.toInternal() } ?: emptyList()
)

private fun BookingUpdateObject.toInternalUpdate() = AjastaBooking(
    id = id?.let { AjastaBookingId(it) } ?: AjastaBookingId.NONE,
    title = title ?: "",
    description = description ?: "",
    lock = lock?.let { AjastaLock(it) } ?: AjastaLock.NONE
)

private fun BookingSlot.toInternal() = AjastaSlot(
    slotStart = slotStart?.let { kotlinx.datetime.Instant.parse(it) } ?: kotlinx.datetime.Instant.DISTANT_PAST,
    slotEnd = slotEnd?.let { kotlinx.datetime.Instant.parse(it) } ?: kotlinx.datetime.Instant.DISTANT_PAST,
    price = price ?: 0.0
)

private fun BookingFilter.toInternal() = AjastaBookingFilter(
    resourceId = resourceId?.let { AjastaResourceId(it) } ?: AjastaResourceId.NONE,
    userId = userId?.let { AjastaUserId(it) } ?: AjastaUserId.NONE,
    status = status.fromTransport(),
    dateFrom = dateFrom?.let { kotlinx.datetime.Instant.parse(it) } ?: kotlinx.datetime.Instant.DISTANT_PAST,
    dateTo = dateTo?.let { kotlinx.datetime.Instant.parse(it) } ?: kotlinx.datetime.Instant.DISTANT_PAST
)

private fun ResourceCreateObject.toInternal() = AjastaResource(
    name = name ?: "",
    type = type.fromTransport(),
    location = location ?: "",
    description = description ?: "",
    imageUrl = imageUrl ?: "",
    pricePerSlot = pricePerSlot ?: 0.0,
    unitsCount = unitsCount ?: 1,
    openTime = openTime ?: "",
    closeTime = closeTime ?: "",
    ownerId = ownerId?.let { AjastaUserId(it) } ?: AjastaUserId.NONE,
    active = active ?: true,
    unavailableWeekdays = unavailableWeekdays ?: "",
    unavailableDates = unavailableDates ?: "",
    dailyUnavailableRanges = dailyUnavailableRanges ?: ""
)

private fun ResourceUpdateObject.toInternalUpdate() = AjastaResource(
    id = id?.let { AjastaResourceId(it) } ?: AjastaResourceId.NONE,
    name = name ?: "",
    type = type.fromTransport(),
    location = location ?: "",
    description = description ?: "",
    imageUrl = imageUrl ?: "",
    pricePerSlot = pricePerSlot ?: 0.0,
    unitsCount = unitsCount ?: 1,
    openTime = openTime ?: "",
    closeTime = closeTime ?: "",
    lock = lock?.let { AjastaLock(it) } ?: AjastaLock.NONE,
    ownerId = ownerId?.let { AjastaUserId(it) } ?: AjastaUserId.NONE,
    active = active ?: true,
    unavailableWeekdays = unavailableWeekdays ?: "",
    unavailableDates = unavailableDates ?: "",
    dailyUnavailableRanges = dailyUnavailableRanges ?: ""
)

private fun ResourceFilter.toInternal() = AjastaResourceFilter(
    type = type.fromTransport(),
    location = location ?: "",
    minPrice = minPrice ?: 0.0,
    maxPrice = maxPrice ?: Double.MAX_VALUE,
    minRating = minRating ?: 0.0,
    ownerId = ownerId?.let { AjastaUserId(it) } ?: AjastaUserId.NONE
)

// === Enum Mappers ===

private fun BookingStatus?.fromTransport() = when (this) {
    BookingStatus.PENDING -> AjastaBookingStatus.PENDING
    BookingStatus.CONFIRMED -> AjastaBookingStatus.CONFIRMED
    BookingStatus.CANCELLED -> AjastaBookingStatus.CANCELLED
    BookingStatus.COMPLETED -> AjastaBookingStatus.COMPLETED
    null -> AjastaBookingStatus.NONE
}

private fun ResourceType?.fromTransport() = when (this) {
    ResourceType.TURF_COURT -> AjastaResourceType.TURF_COURT
    ResourceType.VOLLEYBALL_COURT -> AjastaResourceType.VOLLEYBALL_COURT
    ResourceType.PLAYGROUND -> AjastaResourceType.PLAYGROUND
    ResourceType.HAIRDRESSING_CHAIR -> AjastaResourceType.HAIRDRESSING_CHAIR
    ResourceType.OTHER -> AjastaResourceType.OTHER
    null -> AjastaResourceType.NONE
}

// === Debug Mappers ===

private fun Debug?.toWorkMode() = when (this?.mode) {
    RequestDebugMode.PROD -> AjastaWorkMode.PROD
    RequestDebugMode.TEST -> AjastaWorkMode.TEST
    RequestDebugMode.STUB -> AjastaWorkMode.STUB
    null -> AjastaWorkMode.PROD
}

private fun Debug?.toStubCase() = when (this?.stub) {
    RequestDebugStubs.SUCCESS -> AjastaStubs.SUCCESS
    RequestDebugStubs.NOT_FOUND -> AjastaStubs.NOT_FOUND
    RequestDebugStubs.VALIDATION_ERROR -> AjastaStubs.VALIDATION_ERROR
    RequestDebugStubs.DELETE_ERROR -> AjastaStubs.DELETE_ERROR
    RequestDebugStubs.SEARCH_ERROR -> AjastaStubs.SEARCH_ERROR
    null -> AjastaStubs.NONE
}
