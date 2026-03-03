package top.ajasta.repo

import top.ajasta.common.models.AjastaBooking
import top.ajasta.common.models.AjastaError
import top.ajasta.common.models.AjastaResource

/**
 * Base response interface for repository operations.
 */
sealed interface IDbResponse<T> {
    val data: T
    val errors: List<AjastaError>
}

/**
 * Response for single booking operations.
 */
sealed interface IDbBookingResponse : IDbResponse<AjastaBooking> {
    data class Ok(
        override val data: AjastaBooking
    ) : IDbBookingResponse {
        override val errors: List<AjastaError> = emptyList()
    }

    data class Err(
        override val errors: List<AjastaError> = emptyList()
    ) : IDbBookingResponse {
        override val data: AjastaBooking = AjastaBooking()
    }

    data class ErrWithData(
        override val data: AjastaBooking,
        override val errors: List<AjastaError> = emptyList()
    ) : IDbBookingResponse
}

/**
 * Response for multiple bookings operations (search) with pagination support.
 */
sealed interface IDbBookingsResponse : IDbResponse<List<AjastaBooking>> {
    val total: Int

    data class Ok(
        override val data: List<AjastaBooking>,
        override val total: Int = data.size
    ) : IDbBookingsResponse {
        override val errors: List<AjastaError> = emptyList()
    }

    data class Err(
        override val errors: List<AjastaError> = emptyList()
    ) : IDbBookingsResponse {
        override val data: List<AjastaBooking> = emptyList()
        override val total: Int = 0
    }
}

/**
 * Response for single resource operations.
 */
sealed interface IDbResourceResponse : IDbResponse<AjastaResource> {
    data class Ok(
        override val data: AjastaResource
    ) : IDbResourceResponse {
        override val errors: List<AjastaError> = emptyList()
    }

    data class Err(
        override val errors: List<AjastaError> = emptyList()
    ) : IDbResourceResponse {
        override val data: AjastaResource = AjastaResource()
    }

    data class ErrWithData(
        override val data: AjastaResource,
        override val errors: List<AjastaError> = emptyList()
    ) : IDbResourceResponse
}

/**
 * Response for multiple resources operations (search) with pagination support.
 */
sealed interface IDbResourcesResponse : IDbResponse<List<AjastaResource>> {
    val total: Int

    data class Ok(
        override val data: List<AjastaResource>,
        override val total: Int = data.size
    ) : IDbResourcesResponse {
        override val errors: List<AjastaError> = emptyList()
    }

    data class Err(
        override val errors: List<AjastaError> = emptyList()
    ) : IDbResourcesResponse {
        override val data: List<AjastaResource> = emptyList()
        override val total: Int = 0
    }
}
