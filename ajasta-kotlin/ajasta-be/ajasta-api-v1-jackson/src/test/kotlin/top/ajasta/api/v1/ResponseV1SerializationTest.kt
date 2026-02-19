package top.ajasta.api.v1

import top.ajasta.api.v1.models.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ResponseV1SerializationTest {

    // === Booking Responses ===

    private val bookingCreateResponse = BookingCreateResponse(
        responseType = "createBooking",
        booking = BookingObject(
            id = "booking-123",
            resourceId = "resource-456",
            userId = "user-789",
            title = "Tennis Court Booking",
            description = "Weekly tennis session",
            slots = listOf(
                BookingSlot(
                    slotStart = "2025-03-01T10:00:00Z",
                    slotEnd = "2025-03-01T11:00:00Z",
                    price = 25.0
                )
            ),
            totalAmount = 25.0,
            bookingStatus = BookingStatus.PENDING,
            paymentStatus = PaymentStatus.PENDING
        ),
        paymentLink = "https://payment.example.com/pay/abc123"
    )

    @Test
    fun serializeBookingCreateResponse() {
        val json = apiV1Mapper.writeValueAsString(bookingCreateResponse)

        assertContains(json, Regex("\"id\":\\s*\"booking-123\""))
        assertContains(json, Regex("\"title\":\\s*\"Tennis Court Booking\""))
        assertContains(json, Regex("\"bookingStatus\":\\s*\"PENDING\""))
        assertContains(json, Regex("\"paymentLink\":\\s*\"https://payment.example.com/pay/abc123\""))
        assertContains(json, Regex("\"responseType\":\\s*\"createBooking\""))
    }

    @Test
    fun deserializeBookingCreateResponse() {
        val json = apiV1Mapper.writeValueAsString(bookingCreateResponse)
        val obj = apiV1Mapper.readValue(json, BookingCreateResponse::class.java)

        assertEquals(bookingCreateResponse, obj)
    }

    private val bookingReadResponse = BookingReadResponse(
        responseType = "readBooking",
        booking = BookingObject(
            id = "booking-456",
            resourceId = "resource-123",
            userId = "user-789",
            title = "My Booking",
            bookingStatus = BookingStatus.CONFIRMED,
            paymentStatus = PaymentStatus.COMPLETED,
            totalAmount = 50.0
        )
    )

    @Test
    fun serializeBookingReadResponse() {
        val json = apiV1Mapper.writeValueAsString(bookingReadResponse)

        assertContains(json, Regex("\"id\":\\s*\"booking-456\""))
        assertContains(json, Regex("\"bookingStatus\":\\s*\"CONFIRMED\""))
        assertContains(json, Regex("\"responseType\":\\s*\"readBooking\""))
    }

    @Test
    fun deserializeBookingReadResponse() {
        val json = apiV1Mapper.writeValueAsString(bookingReadResponse)
        val obj = apiV1Mapper.readValue(json, BookingReadResponse::class.java)

        assertEquals(bookingReadResponse, obj)
    }

    private val bookingUpdateResponse = BookingUpdateResponse(
        responseType = "updateBooking",
        booking = BookingObject(
            id = "booking-789",
            title = "Updated Title",
            bookingStatus = BookingStatus.CONFIRMED,
            lock = "new-lock-version"
        )
    )

    @Test
    fun serializeBookingUpdateResponse() {
        val json = apiV1Mapper.writeValueAsString(bookingUpdateResponse)

        assertContains(json, Regex("\"id\":\\s*\"booking-789\""))
        assertContains(json, Regex("\"title\":\\s*\"Updated Title\""))
        assertContains(json, Regex("\"responseType\":\\s*\"updateBooking\""))
    }

    @Test
    fun deserializeBookingUpdateResponse() {
        val json = apiV1Mapper.writeValueAsString(bookingUpdateResponse)
        val obj = apiV1Mapper.readValue(json, BookingUpdateResponse::class.java)

        assertEquals(bookingUpdateResponse, obj)
    }

    private val bookingDeleteResponse = BookingDeleteResponse(
        responseType = "deleteBooking",
        booking = BookingObject(
            id = "booking-999",
            bookingStatus = BookingStatus.CANCELLED
        )
    )

    @Test
    fun serializeBookingDeleteResponse() {
        val json = apiV1Mapper.writeValueAsString(bookingDeleteResponse)

        assertContains(json, Regex("\"id\":\\s*\"booking-999\""))
        assertContains(json, Regex("\"bookingStatus\":\\s*\"CANCELLED\""))
        assertContains(json, Regex("\"responseType\":\\s*\"deleteBooking\""))
    }

    @Test
    fun deserializeBookingDeleteResponse() {
        val json = apiV1Mapper.writeValueAsString(bookingDeleteResponse)
        val obj = apiV1Mapper.readValue(json, BookingDeleteResponse::class.java)

        assertEquals(bookingDeleteResponse, obj)
    }

    private val bookingSearchResponse = BookingSearchResponse(
        responseType = "searchBookings",
        bookings = listOf(
            BookingObject(
                id = "booking-1",
                resourceId = "resource-123",
                title = "Morning Session",
                bookingStatus = BookingStatus.CONFIRMED
            ),
            BookingObject(
                id = "booking-2",
                resourceId = "resource-456",
                title = "Evening Session",
                bookingStatus = BookingStatus.PENDING
            )
        )
    )

    @Test
    fun serializeBookingSearchResponse() {
        val json = apiV1Mapper.writeValueAsString(bookingSearchResponse)

        assertContains(json, Regex("\"responseType\":\\s*\"searchBookings\""))
    }

    @Test
    fun deserializeBookingSearchResponse() {
        val json = apiV1Mapper.writeValueAsString(bookingSearchResponse)
        val obj = apiV1Mapper.readValue(json, BookingSearchResponse::class.java)

        assertEquals(bookingSearchResponse, obj)
    }

    // === Resource Responses ===

    private val resourceCreateResponse = ResourceCreateResponse(
        responseType = "createResource",
        resource = ResourceObject(
            id = "resource-123",
            name = "Tennis Court A",
            type = ResourceType.TURF_COURT,
            location = "Sports Complex, Building 5",
            description = "Professional tennis court",
            pricePerSlot = 30.0,
            unitsCount = 2,
            openTime = "08:00",
            closeTime = "22:00",
            rating = 4.5,
            reviewCount = 10,
            ownerId = "owner-789"
        )
    )

    @Test
    fun serializeResourceCreateResponse() {
        val json = apiV1Mapper.writeValueAsString(resourceCreateResponse)

        assertContains(json, Regex("\"id\":\\s*\"resource-123\""))
        assertContains(json, Regex("\"name\":\\s*\"Tennis Court A\""))
        assertContains(json, Regex("\"type\":\\s*\"TURF_COURT\""))
        assertContains(json, Regex("\"pricePerSlot\":\\s*30\\.0"))
        assertContains(json, Regex("\"responseType\":\\s*\"createResource\""))
    }

    @Test
    fun deserializeResourceCreateResponse() {
        val json = apiV1Mapper.writeValueAsString(resourceCreateResponse)
        val obj = apiV1Mapper.readValue(json, ResourceCreateResponse::class.java)

        assertEquals(resourceCreateResponse, obj)
    }

    private val resourceReadResponse = ResourceReadResponse(
        responseType = "readResource",
        resource = ResourceObject(
            id = "resource-456",
            name = "Volleyball Court B",
            type = ResourceType.VOLLEYBALL_COURT,
            location = "Sports Complex, Building 3",
            pricePerSlot = 25.0,
            rating = 4.8,
            reviewCount = 25
        )
    )

    @Test
    fun serializeResourceReadResponse() {
        val json = apiV1Mapper.writeValueAsString(resourceReadResponse)

        assertContains(json, Regex("\"id\":\\s*\"resource-456\""))
        assertContains(json, Regex("\"type\":\\s*\"VOLLEYBALL_COURT\""))
        assertContains(json, Regex("\"responseType\":\\s*\"readResource\""))
    }

    @Test
    fun deserializeResourceReadResponse() {
        val json = apiV1Mapper.writeValueAsString(resourceReadResponse)
        val obj = apiV1Mapper.readValue(json, ResourceReadResponse::class.java)

        assertEquals(resourceReadResponse, obj)
    }

    private val resourceUpdateResponse = ResourceUpdateResponse(
        responseType = "updateResource",
        resource = ResourceObject(
            id = "resource-789",
            name = "Updated Resource",
            type = ResourceType.HAIRDRESSING_CHAIR,
            pricePerSlot = 40.0,
            lock = "new-lock-version"
        )
    )

    @Test
    fun serializeResourceUpdateResponse() {
        val json = apiV1Mapper.writeValueAsString(resourceUpdateResponse)

        assertContains(json, Regex("\"id\":\\s*\"resource-789\""))
        assertContains(json, Regex("\"name\":\\s*\"Updated Resource\""))
        assertContains(json, Regex("\"responseType\":\\s*\"updateResource\""))
    }

    @Test
    fun deserializeResourceUpdateResponse() {
        val json = apiV1Mapper.writeValueAsString(resourceUpdateResponse)
        val obj = apiV1Mapper.readValue(json, ResourceUpdateResponse::class.java)

        assertEquals(resourceUpdateResponse, obj)
    }

    private val resourceDeleteResponse = ResourceDeleteResponse(
        responseType = "deleteResource",
        resource = ResourceObject(
            id = "resource-999"
        )
    )

    @Test
    fun serializeResourceDeleteResponse() {
        val json = apiV1Mapper.writeValueAsString(resourceDeleteResponse)

        assertContains(json, Regex("\"id\":\\s*\"resource-999\""))
        assertContains(json, Regex("\"responseType\":\\s*\"deleteResource\""))
    }

    @Test
    fun deserializeResourceDeleteResponse() {
        val json = apiV1Mapper.writeValueAsString(resourceDeleteResponse)
        val obj = apiV1Mapper.readValue(json, ResourceDeleteResponse::class.java)

        assertEquals(resourceDeleteResponse, obj)
    }

    private val resourceSearchResponse = ResourceSearchResponse(
        responseType = "searchResources",
        resources = listOf(
            ResourceObject(
                id = "resource-1",
                name = "Tennis Court A",
                type = ResourceType.TURF_COURT,
                pricePerSlot = 30.0,
                rating = 4.5
            ),
            ResourceObject(
                id = "resource-2",
                name = "Tennis Court B",
                type = ResourceType.TURF_COURT,
                pricePerSlot = 35.0,
                rating = 4.7
            )
        )
    )

    @Test
    fun serializeResourceSearchResponse() {
        val json = apiV1Mapper.writeValueAsString(resourceSearchResponse)

        assertContains(json, Regex("\"responseType\":\\s*\"searchResources\""))
    }

    @Test
    fun deserializeResourceSearchResponse() {
        val json = apiV1Mapper.writeValueAsString(resourceSearchResponse)
        val obj = apiV1Mapper.readValue(json, ResourceSearchResponse::class.java)

        assertEquals(resourceSearchResponse, obj)
    }

    // === Availability Response ===

    private val availabilityResponse = AvailabilityResponse(
        responseType = "getAvailability",
        resourceId = "resource-123",
        slots = listOf(
            AvailabilitySlot(
                slotStart = "2025-03-01T10:00:00Z",
                slotEnd = "2025-03-01T11:00:00Z",
                available = true,
                availableUnits = 2,
                price = 30.0
            ),
            AvailabilitySlot(
                slotStart = "2025-03-01T11:00:00Z",
                slotEnd = "2025-03-01T12:00:00Z",
                available = false,
                availableUnits = 0,
                price = 30.0
            )
        )
    )

    @Test
    fun serializeAvailabilityResponse() {
        val json = apiV1Mapper.writeValueAsString(availabilityResponse)

        assertContains(json, Regex("\"resourceId\":\\s*\"resource-123\""))
        assertContains(json, Regex("\"available\":\\s*true"))
        assertContains(json, Regex("\"availableUnits\":\\s*2"))
        assertContains(json, Regex("\"responseType\":\\s*\"getAvailability\""))
    }

    @Test
    fun deserializeAvailabilityResponse() {
        val json = apiV1Mapper.writeValueAsString(availabilityResponse)
        val obj = apiV1Mapper.readValue(json, AvailabilityResponse::class.java)

        assertEquals(availabilityResponse, obj)
    }

    // === Error Response ===

    private val errorResponse = BookingCreateResponse(
        responseType = "createBooking",
        errors = listOf(
            Error(
                code = "validation-error",
                group = "booking",
                field = "slots",
                message = "At least one slot is required"
            )
        )
    )

    @Test
    fun serializeErrorResponse() {
        val json = apiV1Mapper.writeValueAsString(errorResponse)

        assertContains(json, Regex("\"code\":\\s*\"validation-error\""))
        assertContains(json, Regex("\"field\":\\s*\"slots\""))
        assertContains(json, Regex("\"message\":\\s*\"At least one slot is required\""))
    }

    @Test
    fun deserializeErrorResponse() {
        val json = apiV1Mapper.writeValueAsString(errorResponse)
        val obj = apiV1Mapper.readValue(json, BookingCreateResponse::class.java)

        assertEquals(errorResponse, obj)
    }
}
