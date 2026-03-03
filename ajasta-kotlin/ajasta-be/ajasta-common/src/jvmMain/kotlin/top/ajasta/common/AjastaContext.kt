package top.ajasta.common

import kotlinx.datetime.Instant
import top.ajasta.common.models.*

/**
 * Main context for processing booking system requests.
 * Tracks the entire request lifecycle through different stages.
 */
open class AjastaContext(
    // Command and state
    var command: AjastaCommand = AjastaCommand.NONE,
    var state: AjastaState = AjastaState.NONE,
    val errors: MutableList<AjastaError> = mutableListOf(),

    // Debug and work mode
    var workMode: AjastaWorkMode = AjastaWorkMode.PROD,
    var stubCase: AjastaStubs = AjastaStubs.NONE,

    // Request metadata
    var requestId: AjastaRequestId = AjastaRequestId.NONE,
    var timeStart: Instant = Instant.DISTANT_PAST,

    // === Booking request flow ===
    // Incoming request
    var bookingRequest: AjastaBooking = AjastaBooking(),
    var bookingFilterRequest: AjastaBookingFilter = AjastaBookingFilter(),

    // After validation
    var bookingValidating: AjastaBooking = AjastaBooking(),
    var bookingFilterValidating: AjastaBookingFilter = AjastaBookingFilter(),

    // After business rules check
    var bookingValidated: AjastaBooking = AjastaBooking(),
    var bookingFilterValidated: AjastaBookingFilter = AjastaBookingFilter(),

    // Repository operations
    var bookingRepoRead: AjastaBooking = AjastaBooking(),
    var bookingRepoPrepare: AjastaBooking = AjastaBooking(),
    var bookingRepoDone: AjastaBooking = AjastaBooking(),
    var bookingsRepoDone: MutableList<AjastaBooking> = mutableListOf(),

    // Response
    var bookingResponse: AjastaBooking = AjastaBooking(),
    var bookingsResponse: MutableList<AjastaBooking> = mutableListOf(),
    var paymentLink: String = "",
    var refundAmount: Double = 0.0,

    // === Resource request flow ===
    // Incoming request
    var resourceRequest: AjastaResource = AjastaResource(),
    var resourceFilterRequest: AjastaResourceFilter = AjastaResourceFilter(),

    // After validation
    var resourceValidating: AjastaResource = AjastaResource(),
    var resourceFilterValidating: AjastaResourceFilter = AjastaResourceFilter(),

    // After business rules check
    var resourceValidated: AjastaResource = AjastaResource(),
    var resourceFilterValidated: AjastaResourceFilter = AjastaResourceFilter(),

    // Repository operations
    var resourceRepoRead: AjastaResource = AjastaResource(),
    var resourceRepoPrepare: AjastaResource = AjastaResource(),
    var resourceRepoDone: AjastaResource = AjastaResource(),
    var resourcesRepoDone: MutableList<AjastaResource> = mutableListOf(),

    // Response
    var resourceResponse: AjastaResource = AjastaResource(),
    var resourcesResponse: MutableList<AjastaResource> = mutableListOf(),

    // === Availability ===
    var availabilityDateFrom: Instant = Instant.DISTANT_PAST,
    var availabilityDateTo: Instant = Instant.DISTANT_PAST,
    var availabilityResourceId: AjastaResourceId = AjastaResourceId.NONE,
    var availableSlots: MutableList<AjastaSlot> = mutableListOf(),

    // === Pagination ===
    var page: Int = 1,
    var pageSize: Int = 20,
    var total: Int = 0
) {
    /**
     * Adds an error to the context and sets state to FAILING.
     */
    fun addError(
        code: String = "",
        group: String = "",
        field: String = "",
        message: String = "",
        exception: Throwable? = null
    ) {
        errors.add(
            AjastaError(
                code = code,
                group = group,
                field = field,
                message = message,
                exception = exception
            )
        )
        state = AjastaState.FAILING
    }
}
