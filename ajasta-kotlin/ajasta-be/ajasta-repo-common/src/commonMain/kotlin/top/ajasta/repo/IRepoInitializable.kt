package top.ajasta.repo

import top.ajasta.common.models.AjastaBooking
import top.ajasta.common.models.AjastaResource

/**
 * Interface for repositories that can be initialized with test data.
 */
interface IRepoBookingInitializable : IRepoBooking {
    suspend fun initBookings(bookings: Collection<AjastaBooking>)
    suspend fun clearBookings()
    fun save(bookings: Collection<AjastaBooking>): Collection<AjastaBooking>
}

/**
 * Interface for repositories that can be initialized with test data.
 */
interface IRepoResourceInitializable : IRepoResource {
    suspend fun initResources(resources: Collection<AjastaResource>)
    suspend fun clearResources()
    fun save(resources: Collection<AjastaResource>): Collection<AjastaResource>
}
