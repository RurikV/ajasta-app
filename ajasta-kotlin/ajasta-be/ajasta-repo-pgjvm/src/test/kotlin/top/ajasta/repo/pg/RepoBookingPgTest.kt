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
import top.ajasta.repo.tests.runRepoTest
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RepoBookingPgTest {
    companion object {
        private val postgresImage = DockerImageName.parse("postgres:15-alpine")
        private lateinit var container: PostgreSQLContainer<*>
        private lateinit var sqlProperties: SqlProperties
        private lateinit var repo: RepoBookingSql

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
        repo = RepoBookingSql(sqlProperties, randomUuid = { uuidNew.asString() })
        transaction(repo.conn) {
            SchemaUtils.create(repo.bookingTable)
        }
        runBlocking {
            repo.initBookings(initObjects)
        }
    }

    @After
    fun cleanup() {
        runBlocking {
            repo.clearBookings()
        }
        transaction(repo.conn) {
            SchemaUtils.drop(repo.bookingTable)
        }
    }

    @Test
    fun createSuccess() = runRepoTest {
        val createObj = AjastaBooking(
            resourceId = AjastaResourceId("resource-new"),
            userId = AjastaUserId("user-123"),
            title = "create object",
            description = "create object description",
            totalAmount = 150.0,
            bookingStatus = AjastaBookingStatus.PENDING
        )
        val result = repo.createBooking(DbBookingRequest(createObj))
        assertIs<IDbBookingResponse.Ok>(result)
        assertNotEquals(AjastaBookingId.NONE, result.data.id)
        assertEquals(uuidNew.asString(), result.data.lock.asString())
        assertEquals(createObj.title, result.data.title)
        assertEquals(createObj.description, result.data.description)
        assertEquals(createObj.resourceId, result.data.resourceId)
    }

    @Test
    fun readSuccess() = runRepoTest {
        val result = repo.readBooking(DbBookingIdRequest(initObjects[0].id))
        assertIs<IDbBookingResponse.Ok>(result)
        assertEquals(initObjects[0].id, result.data.id)
        assertEquals(initObjects[0].title, result.data.title)
    }

    @Test
    fun readNotFound() = runRepoTest {
        val result = repo.readBooking(DbBookingIdRequest(AjastaBookingId("not-found-id")))
        assertIs<IDbBookingResponse.Err>(result)
    }

    @Test
    fun updateSuccess() = runRepoTest {
        val updateObj = initObjects[1].copy(title = "updated title")
        val result = repo.updateBooking(DbBookingRequest(updateObj))
        assertIs<IDbBookingResponse.Ok>(result)
        assertEquals("updated title", result.data.title)
        assertEquals(lockNew.asString(), result.data.lock.asString())
    }

    @Test
    fun updateConcurrentModification() = runRepoTest {
        val updateObj = initObjects[1].copy(lock = lockBad)
        val result = repo.updateBooking(DbBookingRequest(updateObj))
        assertIs<IDbBookingResponse.ErrWithData>(result)
        assertEquals(initObjects[1].id, result.data.id)
    }

    @Test
    fun updateNotFound() = runRepoTest {
        val updateObj = AjastaBooking(
            id = AjastaBookingId("not-found-id"),
            lock = lockOld,
            title = "not found"
        )
        val result = repo.updateBooking(DbBookingRequest(updateObj))
        assertIs<IDbBookingResponse.Err>(result)
    }

    @Test
    fun deleteSuccess() = runRepoTest {
        val result = repo.deleteBooking(DbBookingIdRequest(initObjects[2].id, lockOld))
        assertIs<IDbBookingResponse.Ok>(result)
        assertEquals(initObjects[2].id, result.data.id)

        val readResult = repo.readBooking(DbBookingIdRequest(initObjects[2].id))
        assertIs<IDbBookingResponse.Err>(readResult)
    }

    @Test
    fun deleteConcurrentModification() = runRepoTest {
        val result = repo.deleteBooking(DbBookingIdRequest(initObjects[2].id, lockBad))
        assertIs<IDbBookingResponse.ErrWithData>(result)
    }

    @Test
    fun searchByStatus() = runRepoTest {
        val result = repo.searchBookings(
            DbBookingFilterRequest(status = AjastaBookingStatus.CONFIRMED)
        )
        assertTrue(result.data.isNotEmpty())
        result.data.forEach {
            assertEquals(AjastaBookingStatus.CONFIRMED, it.bookingStatus)
        }
    }

    @Test
    fun searchByResourceId() = runRepoTest {
        val resourceId = AjastaResourceId("resource-123")
        val result = repo.searchBookings(
            DbBookingFilterRequest(resourceId = resourceId)
        )
        assertTrue(result.data.isNotEmpty())
        result.data.forEach {
            assertEquals(resourceId, it.resourceId)
        }
    }

    @Test
    fun searchByUserId() = runRepoTest {
        val userId = AjastaUserId("user-123")
        val result = repo.searchBookings(
            DbBookingFilterRequest(userId = userId)
        )
        assertTrue(result.data.isNotEmpty())
        result.data.forEach {
            assertEquals(userId, it.userId)
        }
    }
}
