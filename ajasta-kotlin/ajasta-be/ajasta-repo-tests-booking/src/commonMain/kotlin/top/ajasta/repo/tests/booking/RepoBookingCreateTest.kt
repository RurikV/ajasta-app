package top.ajasta.repo.tests.booking

import top.ajasta.common.models.*
import top.ajasta.repo.DbBookingRequest
import top.ajasta.repo.IDbBookingResponse
import top.ajasta.repo.IRepoBookingInitializable
import kotlin.test.*

abstract class RepoBookingCreateTest {
    abstract val repo: IRepoBookingInitializable
    protected open val uuidNew = AjastaBookingId("10000000-0000-0000-0000-000000000001")

    private val createObj = AjastaBooking(
        resourceId = AjastaResourceId("resource-new"),
        userId = AjastaUserId("user-123"),
        title = "create object",
        description = "create object description",
        totalAmount = 150.0,
        bookingStatus = AjastaBookingStatus.PENDING
    )

    @Test
    fun createSuccess() = runRepoTest {
        val result = repo.createBooking(DbBookingRequest(createObj))
        val expected = createObj
        assertIs<IDbBookingResponse.Ok>(result)
        assertNotEquals(AjastaBookingId.NONE, result.data.id)
        assertEquals(uuidNew.asString(), result.data.lock.asString())
        assertEquals(expected.title, result.data.title)
        assertEquals(expected.description, result.data.description)
        assertEquals(expected.resourceId, result.data.resourceId)
        assertNotEquals(AjastaBookingId.NONE, result.data.id)
    }

    companion object : BaseInitBooking("create") {
        override val initObjects: List<AjastaBooking> = emptyList()
    }
}
