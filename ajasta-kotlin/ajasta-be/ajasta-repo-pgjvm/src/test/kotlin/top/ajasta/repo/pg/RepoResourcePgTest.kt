package top.ajasta.repo.pg

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.*
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
import top.ajasta.repo.tests.resource.runRepoTest
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for PostgreSQL Resource repository using TestContainers.
 * These tests require Docker to be running. If Docker is not available, tests will be skipped.
 */
class RepoResourcePgTest {
    companion object {
        private val postgresImage = DockerImageName.parse("postgres:15-alpine")
        private var container: PostgreSQLContainer<*>? = null
        private var sqlProperties: SqlProperties? = null
        private var dockerAvailable = false
        private var repo: RepoResourceSql? = null

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
            try {
                container = PostgreSQLContainer(postgresImage)
                    .withDatabaseName("ajasta_test")
                    .withUsername("postgres")
                    .withPassword("test-pass")
                container?.start()

                sqlProperties = SqlProperties(
                    host = container!!.host,
                    port = container!!.firstMappedPort,
                    user = container!!.username,
                    password = container!!.password,
                    database = container!!.databaseName,
                    schema = "public",
                    bookingsTable = "bookings",
                    resourcesTable = "resources"
                )
                dockerAvailable = true
            } catch (e: Exception) {
                println("Docker not available, skipping PostgreSQL tests: ${e.message}")
                dockerAvailable = false
            }
        }

        @AfterClass
        @JvmStatic
        fun stopContainer() {
            container?.stop()
        }
    }

    @Before
    fun setup() {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        repo = RepoResourceSql(sqlProperties!!, randomUuid = { uuidNew.asString() })
        transaction(repo!!.conn) {
            SchemaUtils.create(repo!!.resourceTable)
        }
        runBlocking {
            repo!!.initResources(initObjects)
        }
    }

    @After
    fun cleanup() {
        if (!dockerAvailable || repo == null) return
        runBlocking {
            repo!!.clearResources()
        }
        transaction(repo!!.conn) {
            SchemaUtils.drop(repo!!.resourceTable)
        }
    }

    @Test
    fun createSuccess() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val createObj = AjastaResource(
            name = "create object",
            description = "create object description",
            type = AjastaResourceType.TURF_COURT,
            location = "Building C",
            pricePerSlot = 150.0,
            rating = 4.8,
            ownerId = AjastaUserId("owner-456")
        )
        val result = repo!!.createResource(DbResourceRequest(createObj))
        assertIs<IDbResourceResponse.Ok>(result)
        assertNotEquals(AjastaResourceId.NONE, result.data.id)
        assertEquals(uuidNew.asString(), result.data.lock.asString())
        assertEquals(createObj.name, result.data.name)
        assertEquals(createObj.description, result.data.description)
        assertEquals(createObj.type, result.data.type)
    }

    @Test
    fun readSuccess() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val result = repo!!.readResource(DbResourceIdRequest(initObjects[0].id))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals(initObjects[0].id, result.data.id)
        assertEquals(initObjects[0].name, result.data.name)
    }

    @Test
    fun readNotFound() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val result = repo!!.readResource(DbResourceIdRequest(AjastaResourceId("not-found-id")))
        assertIs<IDbResourceResponse.Err>(result)
    }

    @Test
    fun updateSuccess() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val updateObj = initObjects[1].copy(name = "updated name")
        val result = repo!!.updateResource(DbResourceRequest(updateObj))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals("updated name", result.data.name)
        assertEquals(lockNew.asString(), result.data.lock.asString())
    }

    @Test
    fun updateConcurrentModification() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val updateObj = initObjects[1].copy(lock = lockBad)
        val result = repo!!.updateResource(DbResourceRequest(updateObj))
        assertIs<IDbResourceResponse.ErrWithData>(result)
        assertEquals(initObjects[1].id, result.data.id)
    }

    @Test
    fun updateNotFound() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val updateObj = AjastaResource(
            id = AjastaResourceId("not-found-id"),
            lock = lockOld,
            name = "not found"
        )
        val result = repo!!.updateResource(DbResourceRequest(updateObj))
        assertIs<IDbResourceResponse.Err>(result)
    }

    @Test
    fun deleteSuccess() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val result = repo!!.deleteResource(DbResourceIdRequest(initObjects[2].id, lockOld))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals(initObjects[2].id, result.data.id)

        val readResult = repo!!.readResource(DbResourceIdRequest(initObjects[2].id))
        assertIs<IDbResourceResponse.Err>(readResult)
    }

    @Test
    fun deleteConcurrentModification() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val result = repo!!.deleteResource(DbResourceIdRequest(initObjects[2].id, lockBad))
        assertIs<IDbResourceResponse.ErrWithData>(result)
    }

    @Test
    fun searchByType() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val result = repo!!.searchResources(
            DbResourceFilterRequest(type = AjastaResourceType.VOLLEYBALL_COURT)
        )
        assertTrue(result.data.isNotEmpty())
        result.data.forEach {
            assertEquals(AjastaResourceType.VOLLEYBALL_COURT, it.type)
        }
    }

    @Test
    fun searchByOwnerId() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val ownerId = AjastaUserId("owner-123")
        val result = repo!!.searchResources(
            DbResourceFilterRequest(ownerId = ownerId)
        )
        assertTrue(result.data.isNotEmpty())
        result.data.forEach {
            assertEquals(ownerId, it.ownerId)
        }
    }

    @Test
    fun searchByLocation() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val result = repo!!.searchResources(
            DbResourceFilterRequest(location = "Building A")
        )
        assertTrue(result.data.isNotEmpty())
        result.data.forEach {
            assertEquals("Building A", it.location)
        }
    }

    // === New Availability Fields Tests ===

    @Test
    fun createResourceWithActiveFalse() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val resource = AjastaResource(
            name = "Inactive Resource",
            type = AjastaResourceType.TURF_COURT,
            pricePerSlot = 50.0,
            active = false
        )
        val result = repo!!.createResource(DbResourceRequest(resource))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals(false, result.data.active)
    }

    @Test
    fun createResourceWithAvailabilityRules() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val resource = AjastaResource(
            name = "Resource with Rules",
            type = AjastaResourceType.VOLLEYBALL_COURT,
            pricePerSlot = 40.0,
            active = true,
            unavailableWeekdays = "0,6",
            unavailableDates = "2025-01-01,2025-12-25",
            dailyUnavailableRanges = "12:00-13:00;17:00-18:00"
        )
        val result = repo!!.createResource(DbResourceRequest(resource))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals(true, result.data.active)
        assertEquals("0,6", result.data.unavailableWeekdays)
        assertEquals("2025-01-01,2025-12-25", result.data.unavailableDates)
        assertEquals("12:00-13:00;17:00-18:00", result.data.dailyUnavailableRanges)
    }

    @Test
    fun updateResourceActiveStatus() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val updateObj = initObjects[1].copy(
            active = false,
            unavailableWeekdays = "1",
            unavailableDates = "2025-07-04",
            dailyUnavailableRanges = "14:00-15:00"
        )
        val result = repo!!.updateResource(DbResourceRequest(updateObj))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals(false, result.data.active)
        assertEquals("1", result.data.unavailableWeekdays)
        assertEquals("2025-07-04", result.data.unavailableDates)
        assertEquals("14:00-15:00", result.data.dailyUnavailableRanges)
    }

    @Test
    fun readResourceReturnsAvailabilityFields() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        // First create a resource with availability rules
        val resource = AjastaResource(
            name = "Test Read Availability",
            type = AjastaResourceType.PLAYGROUND,
            pricePerSlot = 25.0,
            active = false,
            unavailableWeekdays = "0",
            unavailableDates = "2025-12-25",
            dailyUnavailableRanges = "13:00-14:00"
        )
        val createResult = repo!!.createResource(DbResourceRequest(resource))
        assertIs<IDbResourceResponse.Ok>(createResult)

        // Then read it back
        val readResult = repo!!.readResource(DbResourceIdRequest(createResult.data.id))
        assertIs<IDbResourceResponse.Ok>(readResult)
        assertEquals(false, readResult.data.active)
        assertEquals("0", readResult.data.unavailableWeekdays)
        assertEquals("2025-12-25", readResult.data.unavailableDates)
        assertEquals("13:00-14:00", readResult.data.dailyUnavailableRanges)
    }
}
