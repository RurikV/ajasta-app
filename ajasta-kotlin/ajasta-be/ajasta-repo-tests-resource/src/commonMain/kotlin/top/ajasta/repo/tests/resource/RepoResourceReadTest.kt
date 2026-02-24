package top.ajasta.repo.tests.resource

import top.ajasta.common.models.*
import top.ajasta.repo.DbResourceIdRequest
import top.ajasta.repo.IDbResourceResponse
import top.ajasta.repo.IRepoResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

abstract class RepoResourceReadTest {
    abstract val repo: IRepoResource
    protected open val readSucc = initObjects[0]

    @Test
    fun readSuccess() = runRepoTest {
        val result = repo.readResource(DbResourceIdRequest(readSucc.id))

        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals(readSucc, result.data)
    }

    @Test
    fun readNotFound() = runRepoTest {
        val result = repo.readResource(DbResourceIdRequest(notFoundId))

        assertIs<IDbResourceResponse.Err>(result)
        val error = result.errors.find { it.code == "repo-not-found" }
        assertEquals("id", error?.field)
    }

    companion object : BaseInitResource("read") {
        override val initObjects: List<AjastaResource> = listOf(
            createInitTestModel("read")
        )

        val notFoundId = AjastaResourceId("resource-repo-read-notFound")
    }
}
