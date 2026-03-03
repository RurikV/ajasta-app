package top.ajasta.repo.tests.booking

import top.ajasta.common.models.*
import top.ajasta.repo.DbBookingRequest
import top.ajasta.repo.IDbBookingResponse
import top.ajasta.repo.IRepoBooking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

abstract class RepoBookingUpdateTest {
    abstract val repo: IRepoBooking
    protected open val updateSucc = initObjects[0]
    protected open val updateConc = initObjects[1]
    protected val updateIdNotFound = AjastaBookingId("booking-repo-update-not-found")
    protected val lockBad = AjastaLock("20000000-0000-0000-0000-000000000009")
    protected val lockNew = AjastaLock("20000000-0000-0000-0000-000000000002")

    private val reqUpdateSucc by lazy {
        AjastaBooking(
            id = updateSucc.id,
            resourceId = updateSucc.resourceId,
            userId = AjastaUserId("user-456"),
            title = "update object",
            description = "update object description",
            totalAmount = 200.0,
            bookingStatus = AjastaBookingStatus.CONFIRMED,
            lock = updateSucc.lock
        )
    }

    private val reqUpdateNotFound = AjastaBooking(
        id = updateIdNotFound,
        resourceId = AjastaResourceId("resource-new"),
        userId = AjastaUserId("user-123"),
        title = "update object not found",
        description = "update object not found description",
        totalAmount = 100.0,
        bookingStatus = AjastaBookingStatus.PENDING,
        lock = lockNew
    )

    private val reqUpdateConc by lazy {
        AjastaBooking(
            id = updateConc.id,
            resourceId = updateConc.resourceId,
            userId = AjastaUserId("user-123"),
            title = "update object concurrency",
            description = "update object concurrency description",
            totalAmount = 150.0,
            bookingStatus = AjastaBookingStatus.PENDING,
            lock = lockBad
        )
    }

    @Test
    fun updateSuccess() = runRepoTest {
        val result = repo.updateBooking(DbBookingRequest(reqUpdateSucc))
        assertIs<IDbBookingResponse.Ok>(result)
        assertEquals(reqUpdateSucc.id, result.data.id)
        assertEquals(reqUpdateSucc.title, result.data.title)
        assertEquals(reqUpdateSucc.description, result.data.description)
        assertEquals(reqUpdateSucc.bookingStatus, result.data.bookingStatus)
        assertEquals(lockNew, result.data.lock)
    }

    @Test
    fun updateNotFound() = runRepoTest {
        val result = repo.updateBooking(DbBookingRequest(reqUpdateNotFound))
        assertIs<IDbBookingResponse.Err>(result)
        val error = result.errors.find { it.code == "repo-not-found" }
        assertEquals("id", error?.field)
    }

    @Test
    fun updateConcurrencyError() = runRepoTest {
        val result = repo.updateBooking(DbBookingRequest(reqUpdateConc))
        assertIs<IDbBookingResponse.ErrWithData>(result)
        val error = result.errors.find { it.code == "repo-concurrency" }
        assertEquals("lock", error?.field)
        assertEquals(updateConc, result.data)
    }

    companion object : BaseInitBooking("update") {
        override val initObjects: List<AjastaBooking> = listOf(
            createInitTestModel("update"),
            createInitTestModel("updateConc")
        )
    }
}
