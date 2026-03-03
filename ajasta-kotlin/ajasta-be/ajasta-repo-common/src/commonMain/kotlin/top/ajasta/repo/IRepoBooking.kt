package top.ajasta.repo

import top.ajasta.common.models.AjastaError

/**
 * Repository interface for booking operations.
 */
interface IRepoBooking {
    /**
     * Create a new booking.
     */
    suspend fun createBooking(rq: DbBookingRequest): IDbBookingResponse

    /**
     * Read a booking by ID.
     */
    suspend fun readBooking(rq: DbBookingIdRequest): IDbBookingResponse

    /**
     * Update an existing booking.
     */
    suspend fun updateBooking(rq: DbBookingRequest): IDbBookingResponse

    /**
     * Delete a booking by ID.
     */
    suspend fun deleteBooking(rq: DbBookingIdRequest): IDbBookingResponse

    /**
     * Search bookings by filter.
     */
    suspend fun searchBookings(rq: DbBookingFilterRequest): IDbBookingsResponse

    companion object {
        val NONE = object : IRepoBooking {
            override suspend fun createBooking(rq: DbBookingRequest): IDbBookingResponse =
                IDbBookingResponse.Err(listOf(AjastaError.DEFAULT))

            override suspend fun readBooking(rq: DbBookingIdRequest): IDbBookingResponse =
                IDbBookingResponse.Err(listOf(AjastaError.DEFAULT))

            override suspend fun updateBooking(rq: DbBookingRequest): IDbBookingResponse =
                IDbBookingResponse.Err(listOf(AjastaError.DEFAULT))

            override suspend fun deleteBooking(rq: DbBookingIdRequest): IDbBookingResponse =
                IDbBookingResponse.Err(listOf(AjastaError.DEFAULT))

            override suspend fun searchBookings(rq: DbBookingFilterRequest): IDbBookingsResponse =
                IDbBookingsResponse.Err(listOf(AjastaError.DEFAULT))
        }
    }
}
