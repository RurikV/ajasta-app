package top.ajasta.repo.tests.booking

import top.ajasta.common.models.*
import top.ajasta.repo.DbBookingFilterRequest
import top.ajasta.repo.IDbBookingsResponse
import top.ajasta.repo.IRepoBooking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

abstract class RepoBookingSearchTest {
    abstract val repo: IRepoBooking

    protected open val initializedObjects: List<AjastaBooking> = initObjects

    @Test
    fun searchByUserId() = runRepoTest {
        val result = repo.searchBookings(DbBookingFilterRequest(userId = searchUserId))
        assertIs<IDbBookingsResponse.Ok>(result)
        val expected = listOf(initializedObjects[1], initializedObjects[3]).sortedBy { it.id.asString() }
        assertEquals(expected, result.data.sortedBy { it.id.asString() })
    }

    @Test
    fun searchByResourceId() = runRepoTest {
        val result = repo.searchBookings(DbBookingFilterRequest(resourceId = searchResourceId))
        assertIs<IDbBookingsResponse.Ok>(result)
        val expected = listOf(initializedObjects[0], initializedObjects[2]).sortedBy { it.id.asString() }
        assertEquals(expected, result.data.sortedBy { it.id.asString() })
    }

    @Test
    fun searchByStatus() = runRepoTest {
        val result = repo.searchBookings(DbBookingFilterRequest(status = AjastaBookingStatus.CONFIRMED))
        assertIs<IDbBookingsResponse.Ok>(result)
        val expected = listOf(initializedObjects[4]).sortedBy { it.id.asString() }
        assertEquals(expected, result.data.sortedBy { it.id.asString() })
    }

    companion object : BaseInitBooking("search") {
        val searchUserId = AjastaUserId("user-search")
        val searchResourceId = AjastaResourceId("resource-search")

        override val initObjects: List<AjastaBooking> = listOf(
            createInitTestModel("booking1", resourceId = searchResourceId),
            createInitTestModel("booking2", userId = searchUserId),
            createInitTestModel("booking3", resourceId = searchResourceId),
            createInitTestModel("booking4", userId = searchUserId),
            createInitTestModel("booking5", status = AjastaBookingStatus.CONFIRMED)
        )
    }
}
