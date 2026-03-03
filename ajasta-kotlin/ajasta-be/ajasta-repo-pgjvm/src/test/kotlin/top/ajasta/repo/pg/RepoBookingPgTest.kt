package top.ajasta.repo.pg

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import top.ajasta.common.models.AjastaBooking
import top.ajasta.common.models.AjastaBookingId
import top.ajasta.common.models.AjastaBookingStatus
import top.ajasta.common.models.AjastaLock
import top.ajasta.common.models.AjastaResourceId
import top.ajasta.common.models.AjastaUserId
import top.ajasta.repo.DbBookingFilterRequest
import top.ajasta.repo.DbBookingIdRequest
import top.ajasta.repo.DbBookingRequest
import top.ajasta.repo.IDbBookingResponse
import top.ajasta.repo.tests.booking.runRepoTest
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for PostgreSQL Booking repository using TestContainers.
 * These tests require Docker to be running. If Docker is not available, tests will be skipped.
 */
class RepoBookingPgTest {
    companion object {
        private val postgresImage = DockerImageName.parse("postgres:15-alpine")
        private var container: PostgreSQLContainer<*>? = null
        private var sqlProperties: SqlProperties? = null
        private var dockerAvailable = false
        private var repo: RepoBookingSql? = null

        private val uuidNew = AjastaBookingId("10000000-0000-0000-0000-000000000001")
        private val lockOld = AjastaLock("20000000-0000-0000-0000-000000000001")
        private val lockNew = AjastaLock("30000000-0000-0000-0000-000000000001")
        private val lockBad = AjastaLock("20000000-0000-0000-0000-000000000009")

        private fun createInitTestModel(
            suf: String,
            resourceId: AjastaResourceId = AjastaResourceId("resource-123"),
            userId: AjastaUserId = AjastaUserId("user-123"),
            lock: AjastaLock = lockOld,
            status: AjastaBookingStatus = AjastaBookingStatus.PENDING
        ) = AjastaBooking(
            id = AjastaBookingId("booking-repo-test-$suf"),
            resourceId = resourceId,
            userId = userId,
            title = "$suf booking",
            description = "$suf booking description",
            totalAmount = 100.0,
            bookingStatus = status,
            lock = lock
        )

        private val initObjects = listOf(
            createInitTestModel("read"),
            createInitTestModel("update", lock = lockOld),
            createInitTestModel("delete", lock = lockOld),
            createInitTestModel("search-1", status = AjastaBookingStatus.CONFIRMED),
            createInitTestModel("search-2", status = AjastaBookingStatus.CONFIRMED)
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
        repo = RepoBookingSql(sqlProperties!!, randomUuid = { uuidNew.asString() })
        transaction(repo!!.conn) {
            SchemaUtils.create(repo!!.bookingTable)
        }
        runBlocking {
            repo!!.initBookings(initObjects)
        }
    }

    @After
    fun cleanup() {
        if (!dockerAvailable || repo == null) return
        runBlocking {
            repo!!.clearBookings()
        }
        transaction(repo!!.conn) {
            SchemaUtils.drop(repo!!.bookingTable)
        }
    }

    @Test
    fun createSuccess() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val createObj = AjastaBooking(
            resourceId = AjastaResourceId("resource-new"),
            userId = AjastaUserId("user-123"),
            title = "create object",
            description = "create object description",
            totalAmount = 150.0,
            bookingStatus = AjastaBookingStatus.PENDING
        )
        val result = repo!!.createBooking(DbBookingRequest(createObj))
        assertIs<IDbBookingResponse.Ok>(result)
        assertNotEquals(AjastaBookingId.NONE, result.data.id)
        assertEquals(uuidNew.asString(), result.data.lock.asString())
        assertEquals(createObj.title, result.data.title)
        assertEquals(createObj.description, result.data.description)
        assertEquals(createObj.resourceId, result.data.resourceId)
    }

    @Test
    fun readSuccess() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val result = repo!!.readBooking(DbBookingIdRequest(initObjects[0].id))
        assertIs<IDbBookingResponse.Ok>(result)
        assertEquals(initObjects[0].id, result.data.id)
        assertEquals(initObjects[0].title, result.data.title)
    }

    @Test
    fun readNotFound() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val result = repo!!.readBooking(DbBookingIdRequest(AjastaBookingId("not-found-id")))
        assertIs<IDbBookingResponse.Err>(result)
    }

    @Test
    fun updateSuccess() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val updateObj = initObjects[1].copy(title = "updated title")
        val result = repo!!.updateBooking(DbBookingRequest(updateObj))
        assertIs<IDbBookingResponse.Ok>(result)
        assertEquals("updated title", result.data.title)
        assertEquals(lockNew.asString(), result.data.lock.asString())
    }

    @Test
    fun updateConcurrentModification() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val updateObj = initObjects[1].copy(lock = lockBad)
        val result = repo!!.updateBooking(DbBookingRequest(updateObj))
        assertIs<IDbBookingResponse.ErrWithData>(result)
        assertEquals(initObjects[1].id, result.data.id)
    }

    @Test
    fun updateNotFound() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val updateObj = AjastaBooking(
            id = AjastaBookingId("not-found-id"),
            lock = lockOld,
            title = "not found"
        )
        val result = repo!!.updateBooking(DbBookingRequest(updateObj))
        assertIs<IDbBookingResponse.Err>(result)
    }

    @Test
    fun deleteSuccess() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val result = repo!!.deleteBooking(DbBookingIdRequest(initObjects[2].id, lockOld))
        assertIs<IDbBookingResponse.Ok>(result)
        assertEquals(initObjects[2].id, result.data.id)

        val readResult = repo!!.readBooking(DbBookingIdRequest(initObjects[2].id))
        assertIs<IDbBookingResponse.Err>(readResult)
    }

    @Test
    fun deleteConcurrentModification() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val result = repo!!.deleteBooking(DbBookingIdRequest(initObjects[2].id, lockBad))
        assertIs<IDbBookingResponse.ErrWithData>(result)
    }

    @Test
    fun searchByStatus() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val result = repo!!.searchBookings(
            DbBookingFilterRequest(status = AjastaBookingStatus.CONFIRMED)
        )
        assertTrue(result.data.isNotEmpty())
        result.data.forEach {
            assertEquals(AjastaBookingStatus.CONFIRMED, it.bookingStatus)
        }
    }

    @Test
    fun searchByResourceId() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val resourceId = AjastaResourceId("resource-123")
        val result = repo!!.searchBookings(
            DbBookingFilterRequest(resourceId = resourceId)
        )
        assertTrue(result.data.isNotEmpty())
        result.data.forEach {
            assertEquals(resourceId, it.resourceId)
        }
    }

    @Test
    fun searchByUserId() = runRepoTest {
        Assume.assumeTrue("Docker is not available", dockerAvailable)
        val userId = AjastaUserId("user-123")
        val result = repo!!.searchBookings(
            DbBookingFilterRequest(userId = userId)
        )
        assertTrue(result.data.isNotEmpty())
        result.data.forEach {
            assertEquals(userId, it.userId)
        }
    }
}
