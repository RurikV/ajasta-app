package top.ajasta.biz.repo

import top.ajasta.biz.BizContext
import top.ajasta.common.helpers.fail
import top.ajasta.common.models.AjastaState
import top.ajasta.lib.cor.ICorChainDsl
import top.ajasta.lib.cor.worker
import top.ajasta.repo.DbResourceFilterRequest
import top.ajasta.repo.DbResourceIdRequest
import top.ajasta.repo.DbResourceRequest
import top.ajasta.repo.IDbResourceResponse
import top.ajasta.repo.IDbResourcesResponse

fun ICorChainDsl<BizContext>.repoResourceCreate(title: String) = worker {
    this.title = title
    description = "Adding resource to DB"
    on { state == AjastaState.RUNNING }
    handle {
        val request = DbResourceRequest(resourceRepoPrepare)
        when (val result = repoResource.createResource(request)) {
            is IDbResourceResponse.Ok -> resourceRepoDone = result.data
            is IDbResourceResponse.Err -> fail(result.errors)
            is IDbResourceResponse.ErrWithData -> {
                fail(result.errors)
                resourceRepoDone = result.data
            }
        }
    }
}

fun ICorChainDsl<BizContext>.repoResourceRead(title: String) = worker {
    this.title = title
    description = "Reading resource from DB"
    on { state == AjastaState.RUNNING }
    handle {
        val request = DbResourceIdRequest(resourceValidated.id)
        when (val result = repoResource.readResource(request)) {
            is IDbResourceResponse.Ok -> resourceRepoRead = result.data
            is IDbResourceResponse.Err -> fail(result.errors)
            is IDbResourceResponse.ErrWithData -> {
                fail(result.errors)
                resourceRepoRead = result.data
            }
        }
    }
}

fun ICorChainDsl<BizContext>.repoResourceUpdate(title: String) = worker {
    this.title = title
    description = "Updating resource in DB"
    on { state == AjastaState.RUNNING }
    handle {
        val request = DbResourceRequest(resourceRepoPrepare)
        when (val result = repoResource.updateResource(request)) {
            is IDbResourceResponse.Ok -> resourceRepoDone = result.data
            is IDbResourceResponse.Err -> fail(result.errors)
            is IDbResourceResponse.ErrWithData -> {
                fail(result.errors)
                resourceRepoDone = result.data
            }
        }
    }
}

fun ICorChainDsl<BizContext>.repoResourceDelete(title: String) = worker {
    this.title = title
    description = "Deleting resource from DB"
    on { state == AjastaState.RUNNING }
    handle {
        val request = DbResourceIdRequest(resourceValidated.id, lock = resourceValidated.lock)
        when (val result = repoResource.deleteResource(request)) {
            is IDbResourceResponse.Ok -> resourceRepoDone = result.data
            is IDbResourceResponse.Err -> fail(result.errors)
            is IDbResourceResponse.ErrWithData -> {
                fail(result.errors)
                resourceRepoDone = result.data
            }
        }
    }
}

fun ICorChainDsl<BizContext>.repoResourceSearch(title: String) = worker {
    this.title = title
    description = "Searching resources in DB with pagination"
    on { state == AjastaState.RUNNING }
    handle {
        val request = DbResourceFilterRequest(
            type = resourceFilterValidated.type,
            location = resourceFilterValidated.location,
            minPrice = resourceFilterValidated.minPrice,
            maxPrice = resourceFilterValidated.maxPrice,
            minRating = resourceFilterValidated.minRating,
            ownerId = resourceFilterValidated.ownerId,
            page = page,
            pageSize = pageSize
        )
        when (val result = repoResource.searchResources(request)) {
            is IDbResourcesResponse.Ok -> {
                resourcesRepoDone.clear()
                resourcesRepoDone.addAll(result.data)
                total = result.total
            }
            is IDbResourcesResponse.Err -> fail(result.errors)
        }
    }
}
