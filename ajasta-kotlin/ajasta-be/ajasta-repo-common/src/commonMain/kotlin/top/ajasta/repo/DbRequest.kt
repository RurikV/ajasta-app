package top.ajasta.repo

import top.ajasta.common.PaginationDefaults
import top.ajasta.common.models.*

/**
 * Request for create/update booking operations.
 */
data class DbBookingRequest(
    val booking: AjastaBooking
)

/**
 * Request for read/delete booking operations.
 */
data class DbBookingIdRequest(
    val id: AjastaBookingId,
    val lock: AjastaLock = AjastaLock.NONE
) {
    constructor(booking: AjastaBooking) : this(booking.id, booking.lock)
}

/**
 * Request for search bookings operation with pagination support.
 */
data class DbBookingFilterRequest(
    val resourceId: AjastaResourceId = AjastaResourceId.NONE,
    val userId: AjastaUserId = AjastaUserId.NONE,
    val status: AjastaBookingStatus = AjastaBookingStatus.NONE,
    val dateFrom: kotlinx.datetime.Instant = kotlinx.datetime.Instant.DISTANT_PAST,
    val dateTo: kotlinx.datetime.Instant = kotlinx.datetime.Instant.DISTANT_PAST,
    val page: Int = PaginationDefaults.DEFAULT_PAGE,
    val pageSize: Int = PaginationDefaults.DEFAULT_PAGE_SIZE
)

/**
 * Request for create/update resource operations.
 */
data class DbResourceRequest(
    val resource: AjastaResource
)

/**
 * Request for read/delete resource operations.
 */
data class DbResourceIdRequest(
    val id: AjastaResourceId,
    val lock: AjastaLock = AjastaLock.NONE
) {
    constructor(resource: AjastaResource) : this(resource.id, resource.lock)
}

/**
 * Request for search resources operation with pagination support.
 */
data class DbResourceFilterRequest(
    val type: AjastaResourceType = AjastaResourceType.NONE,
    val location: String = "",
    val minPrice: Double = 0.0,
    val maxPrice: Double = Double.MAX_VALUE,
    val minRating: Double = 0.0,
    val ownerId: AjastaUserId = AjastaUserId.NONE,
    val page: Int = PaginationDefaults.DEFAULT_PAGE,
    val pageSize: Int = PaginationDefaults.DEFAULT_PAGE_SIZE
)
