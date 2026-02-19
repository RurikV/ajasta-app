package top.ajasta.repo.inmemory

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import top.ajasta.common.PaginationDefaults
import top.ajasta.common.models.*
import top.ajasta.repo.*

class RepoResourceInMemory(
    private val randomUuid: () -> String = { java.util.UUID.randomUUID().toString() }
) : IRepoResourceInitializable {

    private val mutex = Mutex()
    private val resources = mutableMapOf<String, AjastaResource>()

    override suspend fun initResources(resources: Collection<AjastaResource>) {
        mutex.withLock {
            this.resources.clear()
            resources.forEach { resource ->
                val key = resource.id.takeIf { it != AjastaResourceId.NONE }?.asString() ?: randomUuid()
                this.resources[key] = if (resource.id == AjastaResourceId.NONE) {
                    resource.copy(id = AjastaResourceId(key))
                } else {
                    resource
                }
            }
        }
    }

    override suspend fun clearResources() {
        mutex.withLock {
            resources.clear()
        }
    }

    override fun save(resources: Collection<AjastaResource>): Collection<AjastaResource> {
        kotlinx.coroutines.runBlocking {
            initResources(resources)
        }
        return resources
    }

    override suspend fun createResource(rq: DbResourceRequest): IDbResourceResponse {
        val resource = rq.resource
        val key = randomUuid()
        val newResource = resource.copy(
            id = AjastaResourceId(key),
            lock = AjastaLock(randomUuid())
        )
        mutex.withLock {
            resources[key] = newResource
        }
        return IDbResourceResponse.Ok(newResource)
    }

    override suspend fun readResource(rq: DbResourceIdRequest): IDbResourceResponse {
        val key = rq.id.takeIf { it != AjastaResourceId.NONE }?.asString()
            ?: return errorEmptyId()

        return mutex.withLock {
            resources[key]?.let {
                IDbResourceResponse.Ok(it)
            } ?: errorNotFound(rq.id)
        }
    }

    override suspend fun updateResource(rq: DbResourceRequest): IDbResourceResponse {
        val resource = rq.resource
        val key = resource.id.takeIf { it != AjastaResourceId.NONE }?.asString()
            ?: return errorEmptyId()

        if (resource.lock == AjastaLock.NONE) {
            return errorEmptyLock(resource.id)
        }

        return mutex.withLock {
            val oldResource = resources[key]
            when {
                oldResource == null -> errorNotFound(resource.id)
                oldResource.lock != resource.lock -> errorConcurrency(oldResource, resource.lock)
                else -> {
                    val newResource = resource.copy(lock = AjastaLock(randomUuid()))
                    resources[key] = newResource
                    IDbResourceResponse.Ok(newResource)
                }
            }
        }
    }

    override suspend fun deleteResource(rq: DbResourceIdRequest): IDbResourceResponse {
        val key = rq.id.takeIf { it != AjastaResourceId.NONE }?.asString()
            ?: return errorEmptyId()

        if (rq.lock == AjastaLock.NONE) {
            return errorEmptyLock(rq.id)
        }

        return mutex.withLock {
            val oldResource = resources[key]
            when {
                oldResource == null -> errorNotFound(rq.id)
                oldResource.lock != rq.lock -> errorConcurrency(oldResource, rq.lock)
                else -> {
                    resources.remove(key)
                    IDbResourceResponse.Ok(oldResource)
                }
            }
        }
    }

    override suspend fun searchResources(rq: DbResourceFilterRequest): IDbResourcesResponse {
        return mutex.withLock {
            val allResults = resources.values.filter { resource ->
                (rq.type == AjastaResourceType.NONE || resource.type == rq.type) &&
                (rq.location.isEmpty() || resource.location.contains(rq.location, ignoreCase = true)) &&
                (rq.minPrice <= resource.pricePerSlot) &&
                (rq.maxPrice >= resource.pricePerSlot) &&
                (rq.minRating <= resource.rating) &&
                (rq.ownerId == AjastaUserId.NONE || resource.ownerId == rq.ownerId)
            }
            val total = allResults.size
            val pageSize = rq.pageSize.coerceAtMost(PaginationDefaults.MAX_PAGE_SIZE)
            val offset = (rq.page - 1) * pageSize
            val paginatedResults = allResults
                .drop(offset)
                .take(pageSize)
            IDbResourcesResponse.Ok(data = paginatedResults, total = total)
        }
    }

    private fun errorEmptyId(): IDbResourceResponse = IDbResourceResponse.Err(
        listOf(
            AjastaError(
                code = "repo-empty-id",
                group = "repository",
                field = "id",
                message = "ID must not be empty"
            )
        )
    )

    private fun errorEmptyLock(id: AjastaResourceId): IDbResourceResponse = IDbResourceResponse.Err(
        listOf(
            AjastaError(
                code = "repo-empty-lock",
                group = "repository",
                field = "lock",
                message = "Lock must not be empty for resource ${id.asString()}"
            )
        )
    )

    private fun errorNotFound(id: AjastaResourceId): IDbResourceResponse = IDbResourceResponse.Err(
        listOf(
            AjastaError(
                code = "repo-not-found",
                group = "repository",
                field = "id",
                message = "Resource with ID ${id.asString()} not found"
            )
        )
    )

    private fun errorConcurrency(oldResource: AjastaResource, newLock: AjastaLock): IDbResourceResponse =
        IDbResourceResponse.ErrWithData(
            data = oldResource,
            errors = listOf(
                AjastaError(
                    code = "repo-concurrency",
                    group = "repository",
                    field = "lock",
                    message = "Lock mismatch. Expected: ${oldResource.lock.asString()}, got: ${newLock.asString()}"
                )
            )
        )
}
