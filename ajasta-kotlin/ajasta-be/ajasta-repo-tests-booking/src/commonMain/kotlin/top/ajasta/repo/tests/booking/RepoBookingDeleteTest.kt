package top.ajasta.repo.tests.booking

import top.ajasta.common.models.*
import top.ajasta.repo.DbBookingIdRequest
import top.ajasta.repo.IDbBookingResponse
import top.ajasta.repo.IRepoBooking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

abstract class RepoBookingDeleteTest {
    abstract val repo: IRepoBooking
    protected open val deleteSucc = initObjects[0]
    protected open val deleteConc = initObjects[1]
    protected open val notFoundId = AjastaBookingId("booking-repo-delete-notFound")

    @Test
    fun deleteSuccess() = runRepoTest {
        val lockOld = deleteSucc.lock
        val result = repo.deleteBooking(DbBookingIdRequest(deleteSucc.id, lock = lockOld))
        assertIs<IDbBookingResponse.Ok>(result)
        assertEquals(deleteSucc.title, result.data.title)
        assertEquals(deleteSucc.description, result.data.description)
    }

    @Test
    fun deleteNotFound() = runRepoTest {
        val result = repo.readBooking(DbBookingIdRequest(notFoundId, lock = lockOld))

        assertIs<IDbBookingResponse.Err>(result)
        val error = result.errors.find { it.code == "repo-not-found" }
        assertNotNull(error)
    }

    @Test
    fun deleteConcurrency() = runRepoTest {
        val result = repo.deleteBooking(DbBookingIdRequest(deleteConc.id, lock = lockBad))

        assertIs<IDbBookingResponse.ErrWithData>(result)
        val error = result.errors.find { it.code == "repo-concurrency" }
        assertNotNull(error)
    }

    companion object : BaseInitBooking("delete") {
        override val initObjects: List<AjastaBooking> = listOf(
            createInitTestModel("delete"),
            createInitTestModel("deleteLock")
        )
    }
}
