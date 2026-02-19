package top.ajasta.repo.pg

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import top.ajasta.common.models.AjastaLock
import top.ajasta.common.models.AjastaResource
import top.ajasta.common.models.AjastaResourceId
import top.ajasta.common.models.AjastaResourceType
import top.ajasta.common.models.AjastaUserId
import top.ajasta.repo.DbResourceFilterRequest
import top.ajasta.repo.DbResourceIdRequest
import top.ajasta.repo.DbResourceRequest
import top.ajasta.repo.IDbResourceResponse
import top.ajasta.repo.tests.runRepoTest
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RepoResourcePgTest {
    companion object {
        private val postgresImage = DockerImageName.parse("postgres:15-alpine")
        private lateinit var container: PostgreSQLContainer<*>
        private lateinit var sqlProperties: SqlProperties
        private lateinit var repo: RepoResourceSql

        private val uuidNew = AjastaResourceId("10000000-0000-0000-0000-000000000001")
        private val lockOld = AjastaLock("20000000-0000-0000-0000-000000000001")
        private val lockNew = AjastaLock("30000000-0000-0000-0000-000000000001")
        private val lockBad = AjastaLock("20000000-0000-0000-0000-000000000009")

        private fun createInitTestModel(
            suf: String,
            type: AjastaResourceType = AjastaResourceType.TURF_COURT,
            ownerId: AjastaUserId = AjastaUserId("owner-123"),
            location: String = "Building A",
            lock: AjastaLock = lockOld
        ) = AjastaResource(
            id = AjastaResourceId("resource-repo-test-$suf"),
            name = "$suf resource",
            description = "$suf resource description",
            type = type,
            location = location,
            pricePerSlot = 100.0,
            rating = 4.5,
            ownerId = ownerId,
            lock = lock
        )

        private val initObjects = listOf(
            createInitTestModel("read"),
            createInitTestModel("update", lock = lockOld),
            createInitTestModel("delete", lock = lockOld),
            createInitTestModel("search-1", type = AjastaResourceType.VOLLEYBALL_COURT),
            createInitTestModel("search-2", type = AjastaResourceType.VOLLEYBALL_COURT, location = "Building B")
        )

        @BeforeClass
        @JvmStatic
        fun startContainer() {
            container = PostgreSQLContainer(postgresImage)
                .withDatabaseName("ajasta_test")
                .withUsername("postgres")
                .withPassword("test-pass")
            container.start()

            sqlProperties = SqlProperties(
                host = container.host,
                port = container.firstMappedPort,
                user = container.username,
                password = container.password,
                database = container.databaseName,
                schema = "public",
                bookingsTable = "bookings",
                resourcesTable = "resources"
            )
        }

        @AfterClass
        @JvmStatic
        fun stopContainer() {
            if (::container.isInitialized) {
                container.stop()
            }
        }
    }

    @Before
    fun setup() {
        repo = RepoResourceSql(sqlProperties, randomUuid = { uuidNew.asString() })
        transaction(repo.conn) {
            SchemaUtils.create(repo.resourceTable)
        }
        runBlocking {
            repo.initResources(initObjects)
        }
    }

    @After
    fun cleanup() {
        runBlocking {
            repo.clearResources()
        }
        transaction(repo.conn) {
            SchemaUtils.drop(repo.resourceTable)
        }
    }

    @Test
    fun createSuccess() = runRepoTest {
        val createObj = AjastaResource(
            name = "create object",
            description = "create object description",
            type = AjastaResourceType.TURF_COURT,
            location = "Building C",
            pricePerSlot = 150.0,
            rating = 4.8,
            ownerId = AjastaUserId("owner-456")
        )
        val result = repo.createResource(DbResourceRequest(createObj))
        assertIs<IDbResourceResponse.Ok>(result)
        assertNotEquals(AjastaResourceId.NONE, result.data.id)
        assertEquals(uuidNew.asString(), result.data.lock.asString())
        assertEquals(createObj.name, result.data.name)
        assertEquals(createObj.description, result.data.description)
        assertEquals(createObj.type, result.data.type)
    }

    @Test
    fun readSuccess() = runRepoTest {
        val result = repo.readResource(DbResourceIdRequest(initObjects[0].id))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals(initObjects[0].id, result.data.id)
        assertEquals(initObjects[0].name, result.data.name)
    }

    @Test
    fun readNotFound() = runRepoTest {
        val result = repo.readResource(DbResourceIdRequest(AjastaResourceId("not-found-id")))
        assertIs<IDbResourceResponse.Err>(result)
    }

    @Test
    fun updateSuccess() = runRepoTest {
        val updateObj = initObjects[1].copy(name = "updated name")
        val result = repo.updateResource(DbResourceRequest(updateObj))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals("updated name", result.data.name)
        assertEquals(lockNew.asString(), result.data.lock.asString())
    }

    @Test
    fun updateConcurrentModification() = runRepoTest {
        val updateObj = initObjects[1].copy(lock = lockBad)
        val result = repo.updateResource(DbResourceRequest(updateObj))
        assertIs<IDbResourceResponse.ErrWithData>(result)
        assertEquals(initObjects[1].id, result.data.id)
    }

    @Test
    fun updateNotFound() = runRepoTest {
        val updateObj = AjastaResource(
            id = AjastaResourceId("not-found-id"),
            lock = lockOld,
            name = "not found"
        )
        val result = repo.updateResource(DbResourceRequest(updateObj))
        assertIs<IDbResourceResponse.Err>(result)
    }

    @Test
    fun deleteSuccess() = runRepoTest {
        val result = repo.deleteResource(DbResourceIdRequest(initObjects[2].id, lockOld))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals(initObjects[2].id, result.data.id)

        val readResult = repo.readResource(DbResourceIdRequest(initObjects[2].id))
        assertIs<IDbResourceResponse.Err>(readResult)
    }

    @Test
    fun deleteConcurrentModification() = runRepoTest {
        val result = repo.deleteResource(DbResourceIdRequest(initObjects[2].id, lockBad))
        assertIs<IDbResourceResponse.ErrWithData>(result)
    }

    @Test
    fun searchByType() = runRepoTest {
        val result = repo.searchResources(
            DbResourceFilterRequest(type = AjastaResourceType.VOLLEYBALL_COURT)
        )
        assertTrue(result.data.isNotEmpty())
        result.data.forEach {
            assertEquals(AjastaResourceType.VOLLEYBALL_COURT, it.type)
        }
    }

    @Test
    fun searchByOwnerId() = runRepoTest {
        val ownerId = AjastaUserId("owner-123")
        val result = repo.searchResources(
            DbResourceFilterRequest(ownerId = ownerId)
        )
        assertTrue(result.data.isNotEmpty())
        result.data.forEach {
            assertEquals(ownerId, it.ownerId)
        }
    }

    @Test
    fun searchByLocation() = runRepoTest {
        val result = repo.searchResources(
            DbResourceFilterRequest(location = "Building A")
        )
        assertTrue(result.data.isNotEmpty())
        result.data.forEach {
            assertEquals("Building A", it.location)
        }
    }
}
