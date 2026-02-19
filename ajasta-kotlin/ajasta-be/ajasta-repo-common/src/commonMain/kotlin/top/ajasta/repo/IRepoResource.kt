package top.ajasta.repo

import top.ajasta.common.models.AjastaError

/**
 * Repository interface for resource operations.
 */
interface IRepoResource {
    /**
     * Create a new resource.
     */
    suspend fun createResource(rq: DbResourceRequest): IDbResourceResponse

    /**
     * Read a resource by ID.
     */
    suspend fun readResource(rq: DbResourceIdRequest): IDbResourceResponse

    /**
     * Update an existing resource.
     */
    suspend fun updateResource(rq: DbResourceRequest): IDbResourceResponse

    /**
     * Delete a resource by ID.
     */
    suspend fun deleteResource(rq: DbResourceIdRequest): IDbResourceResponse

    /**
     * Search resources by filter.
     */
    suspend fun searchResources(rq: DbResourceFilterRequest): IDbResourcesResponse

    companion object {
        val NONE = object : IRepoResource {
            override suspend fun createResource(rq: DbResourceRequest): IDbResourceResponse =
                IDbResourceResponse.Err(listOf(AjastaError.DEFAULT))

            override suspend fun readResource(rq: DbResourceIdRequest): IDbResourceResponse =
                IDbResourceResponse.Err(listOf(AjastaError.DEFAULT))

            override suspend fun updateResource(rq: DbResourceRequest): IDbResourceResponse =
                IDbResourceResponse.Err(listOf(AjastaError.DEFAULT))

            override suspend fun deleteResource(rq: DbResourceIdRequest): IDbResourceResponse =
                IDbResourceResponse.Err(listOf(AjastaError.DEFAULT))

            override suspend fun searchResources(rq: DbResourceFilterRequest): IDbResourcesResponse =
                IDbResourcesResponse.Err(listOf(AjastaError.DEFAULT))
        }
    }
}
