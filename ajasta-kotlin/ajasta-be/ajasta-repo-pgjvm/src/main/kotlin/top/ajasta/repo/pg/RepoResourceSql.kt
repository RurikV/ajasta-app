package top.ajasta.repo.pg

import com.benasher44.uuid.uuid4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import top.ajasta.common.models.*
import top.ajasta.repo.*

class RepoResourceSql(
    properties: SqlProperties,
    private val randomUuid: () -> String = { uuid4().toString() }
) : IRepoResource, IRepoResourceInitializable {

    internal val resourceTable = ResourceTable("${properties.schema}.${properties.resourcesTable}")

    private val driver = when {
        properties.url.startsWith("jdbc:postgresql://") -> "org.postgresql.Driver"
        else -> throw IllegalArgumentException("Unknown driver for url ${properties.url}")
    }

    internal val conn = Database.connect(
        properties.url, driver, properties.user, properties.password
    )

    fun clear(): Unit = transaction(conn) {
        resourceTable.deleteAll()
    }

    private fun saveObj(resource: AjastaResource): AjastaResource = transaction(conn) {
        val res = resourceTable
            .insert {
                it.to(resource, randomUuid)
            }
            .resultedValues
            ?.map { resourceTable.from(it) }
        res?.first() ?: throw RuntimeException("DB error: insert statement returned empty result")
    }

    private suspend inline fun <T> transactionWrapper(
        crossinline block: () -> T,
        crossinline handle: (Exception) -> T
    ): T = withContext(Dispatchers.IO) {
        try {
            transaction(conn) { block() }
        } catch (e: Exception) {
            handle(e)
        }
    }

    private suspend inline fun transactionWrapper(
        crossinline block: () -> IDbResourceResponse
    ): IDbResourceResponse = transactionWrapper(block) { IDbResourceResponse.Err(listOf(errorFromException(it))) }

    private fun errorFromException(e: Exception): AjastaError = AjastaError(
        code = "db-error",
        group = "repository",
        message = e.message ?: "Unknown database error",
        exception = e
    )

    private fun errorNotFound(id: AjastaResourceId) = AjastaError(
        code = "not-found",
        group = "repository",
        field = "id",
        message = "Resource with id ${id.asString()} not found"
    )

    private fun errorEmptyId() = AjastaError(
        code = "empty-id",
        group = "repository",
        field = "id",
        message = "Resource id must not be empty"
    )

    private fun errorConcurrent(expected: AjastaLock, actual: AjastaLock) = AjastaError(
        code = "concurrent-modification",
        group = "repository",
        field = "lock",
        message = "Expected lock ${expected.asString()}, but found ${actual.asString()}"
    )

    override fun save(resources: Collection<AjastaResource>): Collection<AjastaResource> =
        resources.map { saveObj(it) }

    override suspend fun initResources(resources: Collection<AjastaResource>) {
        withContext(Dispatchers.IO) {
            transaction(conn) {
                resources.forEach { saveObj(it) }
            }
        }
    }

    override suspend fun clearResources() {
        withContext(Dispatchers.IO) {
            transaction(conn) {
                resourceTable.deleteAll()
            }
        }
    }

    override suspend fun createResource(rq: DbResourceRequest): IDbResourceResponse = transactionWrapper {
        IDbResourceResponse.Ok(saveObj(rq.resource))
    }

    private fun read(id: AjastaResourceId): IDbResourceResponse {
        val res = resourceTable.selectAll().where {
            resourceTable.id eq id.asString()
        }.singleOrNull() ?: return IDbResourceResponse.Err(listOf(errorNotFound(id)))
        return IDbResourceResponse.Ok(resourceTable.from(res))
    }

    override suspend fun readResource(rq: DbResourceIdRequest): IDbResourceResponse = transactionWrapper {
        read(rq.id)
    }

    private suspend fun updateWithLock(
        id: AjastaResourceId,
        lock: AjastaLock,
        block: (AjastaResource) -> IDbResourceResponse
    ): IDbResourceResponse = transactionWrapper {
        if (id == AjastaResourceId.NONE) return@transactionWrapper IDbResourceResponse.Err(listOf(errorEmptyId()))

        val current = resourceTable.selectAll().where { resourceTable.id eq id.asString() }
            .singleOrNull()
            ?.let { resourceTable.from(it) }

        when {
            current == null -> IDbResourceResponse.Err(listOf(errorNotFound(id)))
            current.lock != lock -> IDbResourceResponse.ErrWithData(
                data = current,
                errors = listOf(errorConcurrent(lock, current.lock))
            )
            else -> block(current)
        }
    }

    override suspend fun updateResource(rq: DbResourceRequest): IDbResourceResponse = updateWithLock(rq.resource.id, rq.resource.lock) {
        resourceTable.update(where = { resourceTable.id eq rq.resource.id.asString() }) { row ->
            row.to(rq.resource.copy(lock = AjastaLock(randomUuid())), randomUuid)
        }
        val updated = resourceTable.selectAll().where { resourceTable.id eq rq.resource.id.asString() }
            .singleOrNull()
        updated?.let { IDbResourceResponse.Ok(resourceTable.from(it)) }
            ?: IDbResourceResponse.Err(listOf(errorNotFound(rq.resource.id)))
    }

    override suspend fun deleteResource(rq: DbResourceIdRequest): IDbResourceResponse = updateWithLock(rq.id, rq.lock) {
        resourceTable.deleteWhere { id eq rq.id.asString() }
        IDbResourceResponse.Ok(it)
    }

    override suspend fun searchResources(rq: DbResourceFilterRequest): IDbResourcesResponse =
        transactionWrapper({
            val res = resourceTable.selectAll().where {
                buildList {
                    add(Op.TRUE)
                    if (rq.type != AjastaResourceType.NONE) {
                        add(resourceTable.resourceType eq rq.type.name)
                    }
                    if (rq.ownerId != AjastaUserId.NONE) {
                        add(resourceTable.ownerId eq rq.ownerId.asString())
                    }
                    if (rq.location.isNotBlank()) {
                        add(resourceTable.location eq rq.location)
                    }
                }.reduce { a, b -> a and b }
            }
            IDbResourcesResponse.Ok(data = res.map { resourceTable.from(it) })
        }, {
            IDbResourcesResponse.Err(listOf(errorFromException(it)))
        })

    companion object {
        val NONE: IRepoResource = object : IRepoResource, IRepoResourceInitializable {
            override suspend fun initResources(resources: Collection<AjastaResource>) {}
            override suspend fun clearResources() {}
            override fun save(resources: Collection<AjastaResource>): Collection<AjastaResource> = resources
            override suspend fun createResource(rq: DbResourceRequest) = IDbResourceResponse.Err()
            override suspend fun readResource(rq: DbResourceIdRequest) = IDbResourceResponse.Err()
            override suspend fun updateResource(rq: DbResourceRequest) = IDbResourceResponse.Err()
            override suspend fun deleteResource(rq: DbResourceIdRequest) = IDbResourceResponse.Err()
            override suspend fun searchResources(rq: DbResourceFilterRequest) = IDbResourcesResponse.Err()
        }
    }
}
