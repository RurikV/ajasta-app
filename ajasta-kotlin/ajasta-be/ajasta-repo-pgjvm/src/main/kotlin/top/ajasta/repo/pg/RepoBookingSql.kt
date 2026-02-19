package top.ajasta.repo.pg

import com.benasher44.uuid.uuid4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import top.ajasta.common.PaginationDefaults
import top.ajasta.common.models.*
import top.ajasta.repo.*

class RepoBookingSql(
    properties: SqlProperties,
    private val randomUuid: () -> String = { uuid4().toString() }
) : IRepoBooking, IRepoBookingInitializable {

    internal val bookingTable = BookingTable("${properties.schema}.${properties.bookingsTable}")

    private val driver = when {
        properties.url.startsWith("jdbc:postgresql://") -> "org.postgresql.Driver"
        else -> throw IllegalArgumentException("Unknown driver for url ${properties.url}")
    }

    internal val conn = Database.connect(
        properties.url, driver, properties.user, properties.password
    )

    fun clear(): Unit = transaction(conn) {
        bookingTable.deleteAll()
    }

    private fun saveObj(booking: AjastaBooking): AjastaBooking = transaction(conn) {
        val res = bookingTable
            .insert {
                it.to(booking, randomUuid)
            }
            .resultedValues
            ?.map { bookingTable.from(it) }
        res?.first() ?: throw RuntimeException("DB error: insert statement returned empty result")
    }

    private suspend inline fun <T> transactionWrapper(
        crossinline block: () -> T,
        crossinline handle: (Exception) -> T
    ): T = withContext(Dispatchers.IO) {
        try {
            transaction(conn) { block() }
        } catch (e: Exception) {
            handle(e)
        }
    }

    private suspend inline fun transactionWrapper(
        crossinline block: () -> IDbBookingResponse
    ): IDbBookingResponse = transactionWrapper(block) { IDbBookingResponse.Err(listOf(errorFromException(it))) }

    private fun errorFromException(e: Exception): AjastaError = AjastaError(
        code = "db-error",
        group = "repository",
        message = e.message ?: "Unknown database error",
        exception = e
    )

    private fun errorNotFound(id: AjastaBookingId) = AjastaError(
        code = "not-found",
        group = "repository",
        field = "id",
        message = "Booking with id ${id.asString()} not found"
    )

    private fun errorEmptyId() = AjastaError(
        code = "empty-id",
        group = "repository",
        field = "id",
        message = "Booking id must not be empty"
    )

    private fun errorConcurrent(expected: AjastaLock, actual: AjastaLock) = AjastaError(
        code = "concurrent-modification",
        group = "repository",
        field = "lock",
        message = "Expected lock ${expected.asString()}, but found ${actual.asString()}"
    )

    override fun save(bookings: Collection<AjastaBooking>): Collection<AjastaBooking> =
        bookings.map { saveObj(it) }

    override suspend fun initBookings(bookings: Collection<AjastaBooking>) {
        withContext(Dispatchers.IO) {
            transaction(conn) {
                bookings.forEach { saveObj(it) }
            }
        }
    }

    override suspend fun clearBookings() {
        withContext(Dispatchers.IO) {
            transaction(conn) {
                bookingTable.deleteAll()
            }
        }
    }

    override suspend fun createBooking(rq: DbBookingRequest): IDbBookingResponse = transactionWrapper {
        IDbBookingResponse.Ok(saveObj(rq.booking))
    }

    private fun read(id: AjastaBookingId): IDbBookingResponse {
        val res = bookingTable.selectAll().where {
            bookingTable.id eq id.asString()
        }.singleOrNull() ?: return IDbBookingResponse.Err(listOf(errorNotFound(id)))
        return IDbBookingResponse.Ok(bookingTable.from(res))
    }

    override suspend fun readBooking(rq: DbBookingIdRequest): IDbBookingResponse = transactionWrapper {
        read(rq.id)
    }

    private suspend fun updateWithLock(
        id: AjastaBookingId,
        lock: AjastaLock,
        block: (AjastaBooking) -> IDbBookingResponse
    ): IDbBookingResponse = transactionWrapper {
        if (id == AjastaBookingId.NONE) return@transactionWrapper IDbBookingResponse.Err(listOf(errorEmptyId()))

        val current = bookingTable.selectAll().where { bookingTable.id eq id.asString() }
            .singleOrNull()
            ?.let { bookingTable.from(it) }

        when {
            current == null -> IDbBookingResponse.Err(listOf(errorNotFound(id)))
            current.lock != lock -> IDbBookingResponse.ErrWithData(
                data = current,
                errors = listOf(errorConcurrent(lock, current.lock))
            )
            else -> block(current)
        }
    }

    override suspend fun updateBooking(rq: DbBookingRequest): IDbBookingResponse = updateWithLock(rq.booking.id, rq.booking.lock) {
        bookingTable.update(where = { bookingTable.id eq rq.booking.id.asString() }) { row ->
            row.to(rq.booking.copy(lock = AjastaLock(randomUuid())), randomUuid)
        }
        val updated = bookingTable.selectAll().where { bookingTable.id eq rq.booking.id.asString() }
            .singleOrNull()
        updated?.let { IDbBookingResponse.Ok(bookingTable.from(it)) }
            ?: IDbBookingResponse.Err(listOf(errorNotFound(rq.booking.id)))
    }

    override suspend fun deleteBooking(rq: DbBookingIdRequest): IDbBookingResponse = updateWithLock(rq.id, rq.lock) {
        bookingTable.deleteWhere { id eq rq.id.asString() }
        IDbBookingResponse.Ok(it)
    }

    override suspend fun searchBookings(rq: DbBookingFilterRequest): IDbBookingsResponse =
        transactionWrapper({
            val allResults = bookingTable.selectAll().where {
                buildList {
                    add(Op.TRUE)
                    if (rq.resourceId != AjastaResourceId.NONE) {
                        add(bookingTable.resourceId eq rq.resourceId.asString())
                    }
                    if (rq.userId != AjastaUserId.NONE) {
                        add(bookingTable.userId eq rq.userId.asString())
                    }
                    if (rq.status != AjastaBookingStatus.NONE) {
                        add(bookingTable.status eq rq.status.name)
                    }
                }.reduce { a, b -> a and b }
            }.toList()

            val total = allResults.size
            val pageSize = rq.pageSize.coerceAtMost(PaginationDefaults.MAX_PAGE_SIZE)
            val offset = (rq.page - 1) * pageSize
            val paginatedResults = allResults
                .drop(offset)
                .take(pageSize)
                .map { bookingTable.from(it) }

            IDbBookingsResponse.Ok(data = paginatedResults, total = total)
        }, {
            IDbBookingsResponse.Err(listOf(errorFromException(it)))
        })

    companion object {
        val NONE: IRepoBooking = object : IRepoBooking, IRepoBookingInitializable {
            override suspend fun initBookings(bookings: Collection<AjastaBooking>) {}
            override suspend fun clearBookings() {}
            override fun save(bookings: Collection<AjastaBooking>): Collection<AjastaBooking> = bookings
            override suspend fun createBooking(rq: DbBookingRequest) = IDbBookingResponse.Err()
            override suspend fun readBooking(rq: DbBookingIdRequest) = IDbBookingResponse.Err()
            override suspend fun updateBooking(rq: DbBookingRequest) = IDbBookingResponse.Err()
            override suspend fun deleteBooking(rq: DbBookingIdRequest) = IDbBookingResponse.Err()
            override suspend fun searchBookings(rq: DbBookingFilterRequest) = IDbBookingsResponse.Err()
        }
    }
}
