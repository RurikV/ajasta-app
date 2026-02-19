package top.ajasta.api.v1

import top.ajasta.api.v1.models.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class RequestV1SerializationTest {

    // === Booking Requests ===

    private val bookingCreateRequest = BookingCreateRequest(
        requestType = "createBooking",
        debug = Debug(
            mode = RequestDebugMode.STUB,
            stub = RequestDebugStubs.SUCCESS
        ),
        booking = BookingCreateObject(
            resourceId = "resource-123",
            title = "Tennis Court Booking",
            description = "Weekly tennis session",
            slots = listOf(
                BookingSlot(
                    slotStart = "2025-03-01T10:00:00Z",
                    slotEnd = "2025-03-01T11:00:00Z",
                    price = 25.0
                )
            )
        )
    )

    @Test
    fun serializeBookingCreateRequest() {
        val json = apiV1Mapper.writeValueAsString(bookingCreateRequest)

        assertContains(json, Regex("\"resourceId\":\\s*\"resource-123\""))
        assertContains(json, Regex("\"title\":\\s*\"Tennis Court Booking\""))
        assertContains(json, Regex("\"mode\":\\s*\"STUB\""))
        assertContains(json, Regex("\"requestType\":\\s*\"createBooking\""))
    }

    @Test
    fun deserializeBookingCreateRequest() {
        val json = apiV1Mapper.writeValueAsString(bookingCreateRequest)
        val obj = apiV1Mapper.readValue(json, BookingCreateRequest::class.java)

        assertEquals(bookingCreateRequest, obj)
    }

    private val bookingReadRequest = BookingReadRequest(
        requestType = "readBooking",
        debug = Debug(mode = RequestDebugMode.PROD),
        booking = BookingReadObject(
            id = "booking-456"
        )
    )

    @Test
    fun serializeBookingReadRequest() {
        val json = apiV1Mapper.writeValueAsString(bookingReadRequest)

        assertContains(json, Regex("\"id\":\\s*\"booking-456\""))
        assertContains(json, Regex("\"requestType\":\\s*\"readBooking\""))
    }

    @Test
    fun deserializeBookingReadRequest() {
        val json = apiV1Mapper.writeValueAsString(bookingReadRequest)
        val obj = apiV1Mapper.readValue(json, BookingReadRequest::class.java)

        assertEquals(bookingReadRequest, obj)
    }

    private val bookingUpdateRequest = BookingUpdateRequest(
        requestType = "updateBooking",
        debug = Debug(mode = RequestDebugMode.PROD),
        booking = BookingUpdateObject(
            id = "booking-789",
            title = "Updated Booking Title",
            lock = "lock-version-1"
        )
    )

    @Test
    fun serializeBookingUpdateRequest() {
        val json = apiV1Mapper.writeValueAsString(bookingUpdateRequest)

        assertContains(json, Regex("\"id\":\\s*\"booking-789\""))
        assertContains(json, Regex("\"title\":\\s*\"Updated Booking Title\""))
        assertContains(json, Regex("\"requestType\":\\s*\"updateBooking\""))
    }

    @Test
    fun deserializeBookingUpdateRequest() {
        val json = apiV1Mapper.writeValueAsString(bookingUpdateRequest)
        val obj = apiV1Mapper.readValue(json, BookingUpdateRequest::class.java)

        assertEquals(bookingUpdateRequest, obj)
    }

    private val bookingDeleteRequest = BookingDeleteRequest(
        requestType = "deleteBooking",
        debug = Debug(mode = RequestDebugMode.PROD),
        booking = BookingDeleteObject(
            id = "booking-999",
            lock = "lock-version-2"
        )
    )

    @Test
    fun serializeBookingDeleteRequest() {
        val json = apiV1Mapper.writeValueAsString(bookingDeleteRequest)

        assertContains(json, Regex("\"id\":\\s*\"booking-999\""))
        assertContains(json, Regex("\"requestType\":\\s*\"deleteBooking\""))
    }

    @Test
    fun deserializeBookingDeleteRequest() {
        val json = apiV1Mapper.writeValueAsString(bookingDeleteRequest)
        val obj = apiV1Mapper.readValue(json, BookingDeleteRequest::class.java)

        assertEquals(bookingDeleteRequest, obj)
    }

    private val bookingSearchRequest = BookingSearchRequest(
        requestType = "searchBookings",
        debug = Debug(mode = RequestDebugMode.PROD),
        bookingFilter = BookingFilter(
            resourceId = "resource-123",
            status = BookingStatus.CONFIRMED,
            dateFrom = "2025-03-01T00:00:00Z",
            dateTo = "2025-03-31T23:59:59Z"
        ),
        page = 1,
        pageSize = 20
    )

    @Test
    fun serializeBookingSearchRequest() {
        val json = apiV1Mapper.writeValueAsString(bookingSearchRequest)

        assertContains(json, Regex("\"resourceId\":\\s*\"resource-123\""))
        assertContains(json, Regex("\"status\":\\s*\"CONFIRMED\""))
        assertContains(json, Regex("\"requestType\":\\s*\"searchBookings\""))
    }

    @Test
    fun deserializeBookingSearchRequest() {
        val json = apiV1Mapper.writeValueAsString(bookingSearchRequest)
        val obj = apiV1Mapper.readValue(json, BookingSearchRequest::class.java)

        assertEquals(bookingSearchRequest, obj)
    }

    // === Resource Requests ===

    private val resourceCreateRequest = ResourceCreateRequest(
        requestType = "createResource",
        debug = Debug(
            mode = RequestDebugMode.TEST,
            stub = RequestDebugStubs.SUCCESS
        ),
        resource = ResourceCreateObject(
            name = "Tennis Court A",
            type = ResourceType.TURF_COURT,
            location = "Sports Complex, Building 5",
            description = "Professional tennis court with artificial turf",
            pricePerSlot = 30.0,
            unitsCount = 2,
            openTime = "08:00",
            closeTime = "22:00"
        )
    )

    @Test
    fun serializeResourceCreateRequest() {
        val json = apiV1Mapper.writeValueAsString(resourceCreateRequest)

        assertContains(json, Regex("\"name\":\\s*\"Tennis Court A\""))
        assertContains(json, Regex("\"type\":\\s*\"TURF_COURT\""))
        assertContains(json, Regex("\"pricePerSlot\":\\s*30\\.0"))
        assertContains(json, Regex("\"requestType\":\\s*\"createResource\""))
    }

    @Test
    fun deserializeResourceCreateRequest() {
        val json = apiV1Mapper.writeValueAsString(resourceCreateRequest)
        val obj = apiV1Mapper.readValue(json, ResourceCreateRequest::class.java)

        assertEquals(resourceCreateRequest, obj)
    }

    private val resourceReadRequest = ResourceReadRequest(
        requestType = "readResource",
        debug = Debug(mode = RequestDebugMode.PROD),
        resource = ResourceReadObject(
            id = "resource-123"
        )
    )

    @Test
    fun serializeResourceReadRequest() {
        val json = apiV1Mapper.writeValueAsString(resourceReadRequest)

        assertContains(json, Regex("\"id\":\\s*\"resource-123\""))
        assertContains(json, Regex("\"requestType\":\\s*\"readResource\""))
    }

    @Test
    fun deserializeResourceReadRequest() {
        val json = apiV1Mapper.writeValueAsString(resourceReadRequest)
        val obj = apiV1Mapper.readValue(json, ResourceReadRequest::class.java)

        assertEquals(resourceReadRequest, obj)
    }

    private val resourceUpdateRequest = ResourceUpdateRequest(
        requestType = "updateResource",
        debug = Debug(mode = RequestDebugMode.PROD),
        resource = ResourceUpdateObject(
            id = "resource-456",
            name = "Updated Tennis Court",
            pricePerSlot = 35.0,
            lock = "lock-version-1"
        )
    )

    @Test
    fun serializeResourceUpdateRequest() {
        val json = apiV1Mapper.writeValueAsString(resourceUpdateRequest)

        assertContains(json, Regex("\"id\":\\s*\"resource-456\""))
        assertContains(json, Regex("\"name\":\\s*\"Updated Tennis Court\""))
        assertContains(json, Regex("\"requestType\":\\s*\"updateResource\""))
    }

    @Test
    fun deserializeResourceUpdateRequest() {
        val json = apiV1Mapper.writeValueAsString(resourceUpdateRequest)
        val obj = apiV1Mapper.readValue(json, ResourceUpdateRequest::class.java)

        assertEquals(resourceUpdateRequest, obj)
    }

    private val resourceDeleteRequest = ResourceDeleteRequest(
        requestType = "deleteResource",
        debug = Debug(mode = RequestDebugMode.PROD),
        resource = ResourceDeleteObject(
            id = "resource-789",
            lock = "lock-version-2"
        )
    )

    @Test
    fun serializeResourceDeleteRequest() {
        val json = apiV1Mapper.writeValueAsString(resourceDeleteRequest)

        assertContains(json, Regex("\"id\":\\s*\"resource-789\""))
        assertContains(json, Regex("\"requestType\":\\s*\"deleteResource\""))
    }

    @Test
    fun deserializeResourceDeleteRequest() {
        val json = apiV1Mapper.writeValueAsString(resourceDeleteRequest)
        val obj = apiV1Mapper.readValue(json, ResourceDeleteRequest::class.java)

        assertEquals(resourceDeleteRequest, obj)
    }

    private val resourceSearchRequest = ResourceSearchRequest(
        requestType = "searchResources",
        debug = Debug(mode = RequestDebugMode.PROD),
        resourceFilter = ResourceFilter(
            type = ResourceType.TURF_COURT,
            location = "Sports Complex",
            minPrice = 20.0,
            maxPrice = 50.0,
            minRating = 4.0
        ),
        page = 1,
        pageSize = 10
    )

    @Test
    fun serializeResourceSearchRequest() {
        val json = apiV1Mapper.writeValueAsString(resourceSearchRequest)

        assertContains(json, Regex("\"type\":\\s*\"TURF_COURT\""))
        assertContains(json, Regex("\"minPrice\":\\s*20\\.0"))
        assertContains(json, Regex("\"requestType\":\\s*\"searchResources\""))
    }

    @Test
    fun deserializeResourceSearchRequest() {
        val json = apiV1Mapper.writeValueAsString(resourceSearchRequest)
        val obj = apiV1Mapper.readValue(json, ResourceSearchRequest::class.java)

        assertEquals(resourceSearchRequest, obj)
    }

    // === Availability Request ===

    private val availabilityRequest = AvailabilityRequest(
        requestType = "getAvailability",
        debug = Debug(mode = RequestDebugMode.PROD),
        resourceId = "resource-123",
        dateFrom = "2025-03-01T00:00:00Z",
        dateTo = "2025-03-01T23:59:59Z"
    )

    @Test
    fun serializeAvailabilityRequest() {
        val json = apiV1Mapper.writeValueAsString(availabilityRequest)

        assertContains(json, Regex("\"resourceId\":\\s*\"resource-123\""))
        assertContains(json, Regex("\"requestType\":\\s*\"getAvailability\""))
    }

    @Test
    fun deserializeAvailabilityRequest() {
        val json = apiV1Mapper.writeValueAsString(availabilityRequest)
        val obj = apiV1Mapper.readValue(json, AvailabilityRequest::class.java)

        assertEquals(availabilityRequest, obj)
    }

    // === Naked Deserialization Tests ===

    @Test
    fun deserializeNakedBookingCreateRequest() {
        val jsonString = """
            {"booking": null}
        """.trimIndent()
        val obj = apiV1Mapper.readValue(jsonString, BookingCreateRequest::class.java)

        assertEquals(null, obj.booking)
    }

    @Test
    fun deserializeNakedResourceCreateRequest() {
        val jsonString = """
            {"resource": null}
        """.trimIndent()
        val obj = apiV1Mapper.readValue(jsonString, ResourceCreateRequest::class.java)

        assertEquals(null, obj.resource)
    }
}
