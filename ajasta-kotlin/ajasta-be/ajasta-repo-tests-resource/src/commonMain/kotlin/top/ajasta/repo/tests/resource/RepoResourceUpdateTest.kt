package top.ajasta.repo.tests.resource

import top.ajasta.common.models.*
import top.ajasta.repo.DbResourceRequest
import top.ajasta.repo.IDbResourceResponse
import top.ajasta.repo.IRepoResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

abstract class RepoResourceUpdateTest {
    abstract val repo: IRepoResource
    protected open val updateSucc = initObjects[0]
    protected open val updateConc = initObjects[1]
    protected val updateIdNotFound = AjastaResourceId("resource-repo-update-not-found")
    protected val lockBad = AjastaLock("20000000-0000-0000-0000-000000000009")
    protected val lockNew = AjastaLock("20000000-0000-0000-0000-000000000002")

    private val reqUpdateSucc by lazy {
        AjastaResource(
            id = updateSucc.id,
            name = "update object",
            type = AjastaResourceType.HAIRDRESSING_CHAIR,
            location = "updated location",
            description = "update object description",
            pricePerSlot = 200.0,
            rating = 4.8,
            ownerId = updateSucc.ownerId,
            lock = updateSucc.lock
        )
    }

    private val reqUpdateNotFound = AjastaResource(
        id = updateIdNotFound,
        name = "update object not found",
        type = AjastaResourceType.TURF_COURT,
        location = "not found location",
        description = "update object not found description",
        pricePerSlot = 100.0,
        rating = 4.0,
        ownerId = AjastaUserId("owner-123"),
        lock = lockNew
    )

    private val reqUpdateConc by lazy {
        AjastaResource(
            id = updateConc.id,
            name = "update object concurrency",
            type = updateConc.type,
            location = updateConc.location,
            description = "update object concurrency description",
            pricePerSlot = 150.0,
            rating = 4.5,
            ownerId = updateConc.ownerId,
            lock = lockBad
        )
    }

    @Test
    fun updateSuccess() = runRepoTest {
        val result = repo.updateResource(DbResourceRequest(reqUpdateSucc))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals(reqUpdateSucc.id, result.data.id)
        assertEquals(reqUpdateSucc.name, result.data.name)
        assertEquals(reqUpdateSucc.description, result.data.description)
        assertEquals(reqUpdateSucc.type, result.data.type)
        assertEquals(lockNew, result.data.lock)
    }

    @Test
    fun updateNotFound() = runRepoTest {
        val result = repo.updateResource(DbResourceRequest(reqUpdateNotFound))
        assertIs<IDbResourceResponse.Err>(result)
        val error = result.errors.find { it.code == "repo-not-found" }
        assertEquals("id", error?.field)
    }

    @Test
    fun updateConcurrencyError() = runRepoTest {
        val result = repo.updateResource(DbResourceRequest(reqUpdateConc))
        assertIs<IDbResourceResponse.ErrWithData>(result)
        val error = result.errors.find { it.code == "repo-concurrency" }
        assertEquals("lock", error?.field)
        assertEquals(updateConc, result.data)
    }

    companion object : BaseInitResource("update") {
        override val initObjects: List<AjastaResource> = listOf(
            createInitTestModel("update"),
            createInitTestModel("updateConc")
        )
    }
}
