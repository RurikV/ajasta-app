package top.ajasta.repo.inmemory

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import top.ajasta.common.PaginationDefaults
import top.ajasta.common.models.*
import top.ajasta.repo.*

class RepoBookingInMemory(
    private val randomUuid: () -> String = { java.util.UUID.randomUUID().toString() }
) : IRepoBookingInitializable {

    private val mutex = Mutex()
    private val bookings = mutableMapOf<String, AjastaBooking>()

    override suspend fun initBookings(bookings: Collection<AjastaBooking>) {
        mutex.withLock {
            this.bookings.clear()
            bookings.forEach { booking ->
                val key = booking.id.takeIf { it != AjastaBookingId.NONE }?.asString() ?: randomUuid()
                this.bookings[key] = if (booking.id == AjastaBookingId.NONE) {
                    booking.copy(id = AjastaBookingId(key))
                } else {
                    booking
                }
            }
        }
    }

    override suspend fun clearBookings() {
        mutex.withLock {
            bookings.clear()
        }
    }

    override fun save(bookings: Collection<AjastaBooking>): Collection<AjastaBooking> {
        kotlinx.coroutines.runBlocking {
            initBookings(bookings)
        }
        return bookings
    }

    override suspend fun createBooking(rq: DbBookingRequest): IDbBookingResponse {
        val booking = rq.booking
        val key = randomUuid()
        val newBooking = booking.copy(
            id = AjastaBookingId(key),
            lock = AjastaLock(randomUuid())
        )
        mutex.withLock {
            bookings[key] = newBooking
        }
        return IDbBookingResponse.Ok(newBooking)
    }

    override suspend fun readBooking(rq: DbBookingIdRequest): IDbBookingResponse {
        val key = rq.id.takeIf { it != AjastaBookingId.NONE }?.asString()
            ?: return errorEmptyId()

        return mutex.withLock {
            bookings[key]?.let {
                IDbBookingResponse.Ok(it)
            } ?: errorNotFound(rq.id)
        }
    }

    override suspend fun updateBooking(rq: DbBookingRequest): IDbBookingResponse {
        val booking = rq.booking
        val key = booking.id.takeIf { it != AjastaBookingId.NONE }?.asString()
            ?: return errorEmptyId()

        if (booking.lock == AjastaLock.NONE) {
            return errorEmptyLock(booking.id)
        }

        return mutex.withLock {
            val oldBooking = bookings[key]
            when {
                oldBooking == null -> errorNotFound(booking.id)
                oldBooking.lock != booking.lock -> errorConcurrency(oldBooking, booking.lock)
                else -> {
                    val newBooking = booking.copy(lock = AjastaLock(randomUuid()))
                    bookings[key] = newBooking
                    IDbBookingResponse.Ok(newBooking)
                }
            }
        }
    }

    override suspend fun deleteBooking(rq: DbBookingIdRequest): IDbBookingResponse {
        val key = rq.id.takeIf { it != AjastaBookingId.NONE }?.asString()
            ?: return errorEmptyId()

        if (rq.lock == AjastaLock.NONE) {
            return errorEmptyLock(rq.id)
        }

        return mutex.withLock {
            val oldBooking = bookings[key]
            when {
                oldBooking == null -> errorNotFound(rq.id)
                oldBooking.lock != rq.lock -> errorConcurrency(oldBooking, rq.lock)
                else -> {
                    bookings.remove(key)
                    IDbBookingResponse.Ok(oldBooking)
                }
            }
        }
    }

    override suspend fun searchBookings(rq: DbBookingFilterRequest): IDbBookingsResponse {
        return mutex.withLock {
            val allResults = bookings.values.filter { booking ->
                (rq.resourceId == AjastaResourceId.NONE || booking.resourceId == rq.resourceId) &&
                (rq.userId == AjastaUserId.NONE || booking.userId == rq.userId) &&
                (rq.status == AjastaBookingStatus.NONE || booking.bookingStatus == rq.status)
            }
            val total = allResults.size
            val pageSize = rq.pageSize.coerceAtMost(PaginationDefaults.MAX_PAGE_SIZE)
            val offset = (rq.page - 1) * pageSize
            val paginatedResults = allResults
                .drop(offset)
                .take(pageSize)
            IDbBookingsResponse.Ok(data = paginatedResults, total = total)
        }
    }

    private fun errorEmptyId(): IDbBookingResponse = IDbBookingResponse.Err(
        listOf(
            AjastaError(
                code = "repo-empty-id",
                group = "repository",
                field = "id",
                message = "ID must not be empty"
            )
        )
    )

    private fun errorEmptyLock(id: AjastaBookingId): IDbBookingResponse = IDbBookingResponse.Err(
        listOf(
            AjastaError(
                code = "repo-empty-lock",
                group = "repository",
                field = "lock",
                message = "Lock must not be empty for booking ${id.asString()}"
            )
        )
    )

    private fun errorNotFound(id: AjastaBookingId): IDbBookingResponse = IDbBookingResponse.Err(
        listOf(
            AjastaError(
                code = "repo-not-found",
                group = "repository",
                field = "id",
                message = "Booking with ID ${id.asString()} not found"
            )
        )
    )

    private fun errorConcurrency(oldBooking: AjastaBooking, newLock: AjastaLock): IDbBookingResponse =
        IDbBookingResponse.ErrWithData(
            data = oldBooking,
            errors = listOf(
                AjastaError(
                    code = "repo-concurrency",
                    group = "repository",
                    field = "lock",
                    message = "Lock mismatch. Expected: ${oldBooking.lock.asString()}, got: ${newLock.asString()}"
                )
            )
        )
}
