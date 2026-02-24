package top.ajasta.repo.tests.booking

import top.ajasta.common.models.*
import top.ajasta.repo.DbBookingIdRequest
import top.ajasta.repo.IDbBookingResponse
import top.ajasta.repo.IRepoBooking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

abstract class RepoBookingReadTest {
    abstract val repo: IRepoBooking
    protected open val readSucc = initObjects[0]

    @Test
    fun readSuccess() = runRepoTest {
        val result = repo.readBooking(DbBookingIdRequest(readSucc.id))

        assertIs<IDbBookingResponse.Ok>(result)
        assertEquals(readSucc, result.data)
    }

    @Test
    fun readNotFound() = runRepoTest {
        val result = repo.readBooking(DbBookingIdRequest(notFoundId))

        assertIs<IDbBookingResponse.Err>(result)
        val error = result.errors.find { it.code == "repo-not-found" }
        assertEquals("id", error?.field)
    }

    companion object : BaseInitBooking("read") {
        override val initObjects: List<AjastaBooking> = listOf(
            createInitTestModel("read")
        )

        val notFoundId = AjastaBookingId("booking-repo-read-notFound")
    }
}
