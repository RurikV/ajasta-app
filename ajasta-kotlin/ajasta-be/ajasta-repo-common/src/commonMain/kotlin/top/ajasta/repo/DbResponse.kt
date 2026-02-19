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
 * Response for multiple bookings operations (search).
 */
sealed interface IDbBookingsResponse : IDbResponse<List<AjastaBooking>> {
    data class Ok(
        override val data: List<AjastaBooking>
    ) : IDbBookingsResponse {
        override val errors: List<AjastaError> = emptyList()
    }

    data class Err(
        override val errors: List<AjastaError> = emptyList()
    ) : IDbBookingsResponse {
        override val data: List<AjastaBooking> = emptyList()
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
 * Response for multiple resources operations (search).
 */
sealed interface IDbResourcesResponse : IDbResponse<List<AjastaResource>> {
    data class Ok(
        override val data: List<AjastaResource>
    ) : IDbResourcesResponse {
        override val errors: List<AjastaError> = emptyList()
    }

    data class Err(
        override val errors: List<AjastaError> = emptyList()
    ) : IDbResourcesResponse {
        override val data: List<AjastaResource> = emptyList()
    }
}
