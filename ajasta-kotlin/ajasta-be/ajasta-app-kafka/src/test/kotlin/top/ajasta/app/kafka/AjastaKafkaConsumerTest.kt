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

class AjastaKafkaConsumerTest {

    private val processor = AjastaStubProcessor()

    @Test
    fun `deserialize booking create request via mapper`() {
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

        // Verify we can deserialize the JSON correctly
        val deserialized = apiV1Mapper.readValue(json, BookingCreateRequest::class.java)

        assertEquals("createBooking", deserialized.requestType)
        assertEquals("resource-123", deserialized.booking?.resourceId)
    }

    @Test
    fun `deserialize resource create request via mapper`() {
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

        assertEquals("createResource", deserialized.requestType)
        assertEquals("Tennis Court", deserialized.resource?.name)
    }

    @Test
    fun `serialize booking create response`() {
        val response = BookingCreateResponse(
            responseType = "createBooking",
            booking = BookingObject(
                id = "booking-123",
                title = "Test Booking"
            ),
            paymentLink = "https://payment.example.com/pay/123"
        )

        val json = apiV1Mapper.writeValueAsString(response)

        assertTrue(json.contains("\"responseType\":\"createBooking\""))
        assertTrue(json.contains("\"id\":\"booking-123\""))
        assertTrue(json.contains("\"paymentLink\""))
    }

    @Test
    fun `serialize resource search response`() {
        val response = ResourceSearchResponse(
            responseType = "searchResources",
            resources = listOf(
                ResourceObject(
                    id = "resource-1",
                    name = "Tennis Court A",
                    type = ResourceType.TURF_COURT
                ),
                ResourceObject(
                    id = "resource-2",
                    name = "Volleyball Court",
                    type = ResourceType.VOLLEYBALL_COURT
                )
            )
        )

        val json = apiV1Mapper.writeValueAsString(response)

        assertTrue(json.contains("\"responseType\":\"searchResources\""))
    }

    @Test
    fun `process booking request end to end`() = runBlocking {
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
    fun `process resource search end to end`() = runBlocking {
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
}
