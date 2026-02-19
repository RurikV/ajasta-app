package top.ajasta.repo.tests

import top.ajasta.common.models.*
import top.ajasta.repo.DbResourceFilterRequest
import top.ajasta.repo.IDbResourcesResponse
import top.ajasta.repo.IRepoResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

abstract class RepoResourceSearchTest {
    abstract val repo: IRepoResource

    protected open val initializedObjects: List<AjastaResource> = initObjects

    @Test
    fun searchByOwnerId() = runRepoTest {
        val result = repo.searchResources(DbResourceFilterRequest(ownerId = searchOwnerId))
        assertIs<IDbResourcesResponse.Ok>(result)
        val expected = listOf(initializedObjects[1], initializedObjects[3]).sortedBy { it.id.asString() }
        assertEquals(expected, result.data.sortedBy { it.id.asString() })
    }

    @Test
    fun searchByType() = runRepoTest {
        val result = repo.searchResources(DbResourceFilterRequest(type = AjastaResourceType.HAIRDRESSING_CHAIR))
        assertIs<IDbResourcesResponse.Ok>(result)
        val expected = listOf(initializedObjects[2], initializedObjects[4]).sortedBy { it.id.asString() }
        assertEquals(expected, result.data.sortedBy { it.id.asString() })
    }

    @Test
    fun searchByLocation() = runRepoTest {
        val result = repo.searchResources(DbResourceFilterRequest(location = "search"))
        assertIs<IDbResourcesResponse.Ok>(result)
        val expected = listOf(initializedObjects[3], initializedObjects[4]).sortedBy { it.id.asString() }
        assertEquals(expected, result.data.sortedBy { it.id.asString() })
    }

    companion object : BaseInitResource("search") {
        val searchOwnerId = AjastaUserId("owner-search")

        override val initObjects: List<AjastaResource> = listOf(
            createInitTestModel("resource1"),
            createInitTestModel("resource2", ownerId = searchOwnerId),
            createInitTestModel("resource3", type = AjastaResourceType.HAIRDRESSING_CHAIR),
            AjastaResource(
                id = AjastaResourceId("resource-repo-search-resource4"),
                name = "resource4",
                type = AjastaResourceType.TURF_COURT,
                location = "search location",
                description = "resource4 resource description",
                pricePerSlot = 100.0,
                rating = 4.5,
                ownerId = searchOwnerId,
                lock = lockOld
            ),
            AjastaResource(
                id = AjastaResourceId("resource-repo-search-resource5"),
                name = "resource5",
                type = AjastaResourceType.HAIRDRESSING_CHAIR,
                location = "Location search",
                description = "resource5 resource description",
                pricePerSlot = 100.0,
                rating = 4.5,
                ownerId = AjastaUserId("owner-123"),
                lock = lockOld
            )
        )
    }
}
