package top.ajasta.repo.tests.resource

import top.ajasta.common.models.*
import top.ajasta.repo.DbResourceRequest
import top.ajasta.repo.IDbResourceResponse
import top.ajasta.repo.IRepoResourceInitializable
import kotlin.test.*

abstract class RepoResourceCreateTest {
    abstract val repo: IRepoResourceInitializable
    protected open val uuidNew = AjastaResourceId("10000000-0000-0000-0000-000000000001")

    private val createObj = AjastaResource(
        name = "create object",
        type = AjastaResourceType.HAIRDRESSING_CHAIR,
        location = "Location new",
        description = "create object description",
        pricePerSlot = 150.0,
        ownerId = AjastaUserId("owner-123"),
        rating = 4.0
    )

    @Test
    fun createSuccess() = runRepoTest {
        val result = repo.createResource(DbResourceRequest(createObj))
        val expected = createObj
        assertIs<IDbResourceResponse.Ok>(result)
        assertNotEquals(AjastaResourceId.NONE, result.data.id)
        assertEquals(uuidNew.asString(), result.data.lock.asString())
        assertEquals(expected.name, result.data.name)
        assertEquals(expected.description, result.data.description)
        assertEquals(expected.type, result.data.type)
        assertNotEquals(AjastaResourceId.NONE, result.data.id)
    }

    companion object : BaseInitResource("create") {
        override val initObjects: List<AjastaResource> = emptyList()
    }
}
