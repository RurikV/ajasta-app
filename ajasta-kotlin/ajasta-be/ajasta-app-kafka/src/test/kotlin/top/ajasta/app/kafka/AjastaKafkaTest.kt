package top.ajasta.app.kafka

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import top.ajasta.api.v1.apiV1Mapper
import top.ajasta.api.v1.models.*
import top.ajasta.app.common.AjastaStubProcessor
import top.ajasta.biz.BizContext
import top.ajasta.api.v1.mappers.fromTransport
import top.ajasta.api.v1.mappers.toTransport
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AjastaKafkaTest {

    private val processor = AjastaStubProcessor()

    @Test
    fun `process booking create request through context`() = runBlocking {
        val request = BookingCreateRequest(
            requestType = "createBooking",
            booking = BookingCreateObject(
                resourceId = "resource-123",
                title = "Test Booking",
                slots = listOf(
                    BookingSlot(
                        slotStart = "2025-03-01T10:00:00Z",
                        slotEnd = "2025-03-01T11:00:00Z",
                        price = 30.0
                    )
                )
            )
        )

        val ctx = BizContext()
        ctx.fromTransport(request)
        processor.exec(ctx)
        val response = ctx.toTransport() as BookingCreateResponse

        assertNotNull(response.booking)
        assertEquals("createBooking", response.responseType)
        assertNotNull(response.paymentLink)
    }

    @Test
    fun `process resource search request through context`() = runBlocking {
        val request = ResourceSearchRequest(
            requestType = "searchResources",
            resourceFilter = ResourceFilter(
                type = ResourceType.TURF_COURT
            )
        )

        val ctx = BizContext()
        ctx.fromTransport(request)
        processor.exec(ctx)
        val response = ctx.toTransport() as ResourceSearchResponse

        assertTrue(response.resources?.isNotEmpty() == true)
        assertEquals("searchResources", response.responseType)
    }

    @Test
    fun `serialize and deserialize booking request`() {
        val request = BookingCreateRequest(
            requestType = "createBooking",
            booking = BookingCreateObject(
                resourceId = "resource-123",
                title = "Test Booking",
                slots = listOf(
                    BookingSlot(
                        slotStart = "2025-03-01T10:00:00Z",
                        slotEnd = "2025-03-01T11:00:00Z",
                        price = 30.0
                    )
                )
            )
        )

        val json = apiV1Mapper.writeValueAsString(request)
        val deserialized = apiV1Mapper.readValue(json, BookingCreateRequest::class.java)

        assertEquals(request.requestType, deserialized.requestType)
        assertEquals(request.booking?.resourceId, deserialized.booking?.resourceId)
        assertEquals(request.booking?.title, deserialized.booking?.title)
    }

    @Test
    fun `serialize and deserialize resource request`() {
        val request = ResourceCreateRequest(
            requestType = "createResource",
            resource = ResourceCreateObject(
                name = "Tennis Court",
                type = ResourceType.TURF_COURT,
                pricePerSlot = 30.0
            )
        )

        val json = apiV1Mapper.writeValueAsString(request)
        val deserialized = apiV1Mapper.readValue(json, ResourceCreateRequest::class.java)

        assertEquals(request.requestType, deserialized.requestType)
        assertEquals(request.resource?.name, deserialized.resource?.name)
    }

    @Test
    fun `kafka config creates valid properties`() {
        val config = AjastaKafkaConfig(
            processor = processor,
            bootstrapServers = "test:9092",
            groupId = "test-group"
        )

        assertEquals("test:9092", config.bootstrapServers)
        assertEquals("test-group", config.groupId)
        assertEquals("ajasta-request", config.inputTopic)
        assertEquals("ajasta-response", config.outputTopic)
    }
}
