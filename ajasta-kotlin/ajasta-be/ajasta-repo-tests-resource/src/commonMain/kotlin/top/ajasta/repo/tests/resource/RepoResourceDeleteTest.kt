package top.ajasta.repo.tests.resource

import top.ajasta.common.models.*
import top.ajasta.repo.DbResourceIdRequest
import top.ajasta.repo.IDbResourceResponse
import top.ajasta.repo.IRepoResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

abstract class RepoResourceDeleteTest {
    abstract val repo: IRepoResource
    protected open val deleteSucc = initObjects[0]
    protected open val deleteConc = initObjects[1]
    protected open val notFoundId = AjastaResourceId("resource-repo-delete-notFound")

    @Test
    fun deleteSuccess() = runRepoTest {
        val lockOld = deleteSucc.lock
        val result = repo.deleteResource(DbResourceIdRequest(deleteSucc.id, lock = lockOld))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals(deleteSucc.name, result.data.name)
        assertEquals(deleteSucc.description, result.data.description)
    }

    @Test
    fun deleteNotFound() = runRepoTest {
        val result = repo.readResource(DbResourceIdRequest(notFoundId, lock = lockOld))

        assertIs<IDbResourceResponse.Err>(result)
        val error = result.errors.find { it.code == "repo-not-found" }
        assertNotNull(error)
    }

    @Test
    fun deleteConcurrency() = runRepoTest {
        val result = repo.deleteResource(DbResourceIdRequest(deleteConc.id, lock = lockBad))

        assertIs<IDbResourceResponse.ErrWithData>(result)
        val error = result.errors.find { it.code == "repo-concurrency" }
        assertNotNull(error)
    }

    companion object : BaseInitResource("delete") {
        override val initObjects: List<AjastaResource> = listOf(
            createInitTestModel("delete"),
            createInitTestModel("deleteLock")
        )
    }
}
