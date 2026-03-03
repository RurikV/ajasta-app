package top.ajasta.api.v1.mappers

import org.junit.jupiter.api.Test
import top.ajasta.api.v1.models.*
import top.ajasta.common.AjastaContext
import top.ajasta.common.models.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BookingMapperTest {

    // === Create Booking Tests ===

    @Test
    fun `should map BookingCreateRequest to context`() {
        val request = BookingCreateRequest(
            requestId = "req-123",
            debug = Debug(
                mode = RequestDebugMode.STUB,
                stub = RequestDebugStubs.SUCCESS
            ),
            booking = BookingCreateObject(
                resourceId = "resource-456",
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

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaCommand.CREATE_BOOKING, context.command)
        assertEquals("req-123", context.requestId.asString())
        assertEquals(AjastaWorkMode.STUB, context.workMode)
        assertEquals(AjastaStubs.SUCCESS, context.stubCase)
        assertEquals("resource-456", context.bookingRequest.resourceId.asString())
        assertEquals("Tennis Court Booking", context.bookingRequest.title)
        assertEquals("Weekly tennis session", context.bookingRequest.description)
        assertEquals(1, context.bookingRequest.slots.size)
        assertEquals(25.0, context.bookingRequest.slots[0].price)
    }

    @Test
    fun `should map context to BookingCreateResponse`() {
        val context = AjastaContext(
            command = AjastaCommand.CREATE_BOOKING,
            requestId = AjastaRequestId("req-123"),
            bookingResponse = AjastaBooking(
                id = AjastaBookingId("booking-789"),
                resourceId = AjastaResourceId("resource-456"),
                userId = AjastaUserId("user-999"),
                title = "Tennis Court Booking",
                totalAmount = 25.0,
                bookingStatus = AjastaBookingStatus.PENDING,
                paymentStatus = AjastaPaymentStatus.PENDING
            ),
            paymentLink = "https://payment.example.com/pay/abc123"
        )

        val response = context.toTransport() as BookingCreateResponse

        assertEquals("createBooking", response.responseType)
        assertEquals("req-123", response.requestId)
        assertNotNull(response.booking)
        assertEquals("booking-789", response.booking?.id)
        assertEquals("resource-456", response.booking?.resourceId)
        assertEquals("Tennis Court Booking", response.booking?.title)
        assertEquals(BookingStatus.PENDING, response.booking?.bookingStatus)
        assertEquals("https://payment.example.com/pay/abc123", response.paymentLink)
    }

    // === Read Booking Tests ===

    @Test
    fun `should map BookingReadRequest to context`() {
        val request = BookingReadRequest(
            requestId = "req-123",
            booking = BookingReadObject(
                id = "booking-456"
            ),
            debug = Debug(mode = RequestDebugMode.PROD)
        )

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaCommand.READ_BOOKING, context.command)
        assertEquals("booking-456", context.bookingRequest.id.asString())
        assertEquals(AjastaWorkMode.PROD, context.workMode)
    }

    @Test
    fun `should map context to BookingReadResponse`() {
        val context = AjastaContext(
            command = AjastaCommand.READ_BOOKING,
            requestId = AjastaRequestId("req-123"),
            bookingResponse = AjastaBooking(
                id = AjastaBookingId("booking-456"),
                title = "My Booking",
                bookingStatus = AjastaBookingStatus.CONFIRMED,
                paymentStatus = AjastaPaymentStatus.COMPLETED
            )
        )

        val response = context.toTransport() as BookingReadResponse

        assertEquals("readBooking", response.responseType)
        assertEquals("booking-456", response.booking?.id)
        assertEquals(BookingStatus.CONFIRMED, response.booking?.bookingStatus)
    }

    // === Update Booking Tests ===

    @Test
    fun `should map BookingUpdateRequest to context`() {
        val request = BookingUpdateRequest(
            requestId = "req-123",
            booking = BookingUpdateObject(
                id = "booking-789",
                title = "Updated Title",
                description = "Updated Description",
                lock = "lock-v1"
            )
        )

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaCommand.UPDATE_BOOKING, context.command)
        assertEquals("booking-789", context.bookingRequest.id.asString())
        assertEquals("Updated Title", context.bookingRequest.title)
        assertEquals("Updated Description", context.bookingRequest.description)
        assertEquals("lock-v1", context.bookingRequest.lock.asString())
    }

    @Test
    fun `should map context to BookingUpdateResponse`() {
        val context = AjastaContext(
            command = AjastaCommand.UPDATE_BOOKING,
            requestId = AjastaRequestId("req-123"),
            bookingResponse = AjastaBooking(
                id = AjastaBookingId("booking-789"),
                title = "Updated Title",
                lock = AjastaLock("lock-v2")
            )
        )

        val response = context.toTransport() as BookingUpdateResponse

        assertEquals("updateBooking", response.responseType)
        assertEquals("Updated Title", response.booking?.title)
        assertEquals("lock-v2", response.booking?.lock)
    }

    // === Delete Booking Tests ===

    @Test
    fun `should map BookingDeleteRequest to context`() {
        val request = BookingDeleteRequest(
            requestId = "req-123",
            booking = BookingDeleteObject(
                id = "booking-999",
                lock = "lock-v1"
            )
        )

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaCommand.DELETE_BOOKING, context.command)
        assertEquals("booking-999", context.bookingRequest.id.asString())
        assertEquals("lock-v1", context.bookingRequest.lock.asString())
    }

    @Test
    fun `should map context to BookingDeleteResponse`() {
        val context = AjastaContext(
            command = AjastaCommand.DELETE_BOOKING,
            requestId = AjastaRequestId("req-123"),
            bookingResponse = AjastaBooking(
                id = AjastaBookingId("booking-999"),
                bookingStatus = AjastaBookingStatus.CANCELLED
            )
        )

        val response = context.toTransport() as BookingDeleteResponse

        assertEquals("deleteBooking", response.responseType)
        assertEquals("booking-999", response.booking?.id)
        assertEquals(BookingStatus.CANCELLED, response.booking?.bookingStatus)
    }

    // === Search Bookings Tests ===

    @Test
    fun `should map BookingSearchRequest to context`() {
        val request = BookingSearchRequest(
            requestId = "req-123",
            bookingFilter = BookingFilter(
                resourceId = "resource-123",
                status = BookingStatus.CONFIRMED
            ),
            page = 2,
            pageSize = 50
        )

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaCommand.SEARCH_BOOKINGS, context.command)
        assertEquals("resource-123", context.bookingFilterRequest.resourceId.asString())
        assertEquals(AjastaBookingStatus.CONFIRMED, context.bookingFilterRequest.status)
        assertEquals(2, context.page)
        assertEquals(50, context.pageSize)
    }

    @Test
    fun `should map context to BookingSearchResponse`() {
        val context = AjastaContext(
            command = AjastaCommand.SEARCH_BOOKINGS,
            requestId = AjastaRequestId("req-123"),
            bookingsResponse = mutableListOf(
                AjastaBooking(
                    id = AjastaBookingId("booking-1"),
                    title = "Morning Session"
                ),
                AjastaBooking(
                    id = AjastaBookingId("booking-2"),
                    title = "Evening Session"
                )
            )
        )

        val response = context.toTransport() as BookingSearchResponse

        assertEquals("searchBookings", response.responseType)
        assertEquals(2, response.bookings?.size)
    }

    // === Error Mapping Tests ===

    @Test
    fun `should map errors in response`() {
        val context = AjastaContext(
            command = AjastaCommand.CREATE_BOOKING,
            requestId = AjastaRequestId("req-123"),
            errors = mutableListOf(
                AjastaError(
                    code = "validation-error",
                    group = "booking",
                    field = "slots",
                    message = "At least one slot is required"
                )
            )
        )

        val response = context.toTransport() as BookingCreateResponse

        assertEquals(1, response.errors?.size)
        assertEquals("validation-error", response.errors?.first()?.code)
        assertEquals("booking", response.errors?.first()?.group)
        assertEquals("slots", response.errors?.first()?.field)
        assertEquals("At least one slot is required", response.errors?.first()?.message)
    }

    // === Null Handling Tests ===

    @Test
    fun `should handle null values in create request`() {
        val request = BookingCreateRequest(
            booking = null,
            debug = null
        )

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaBooking(), context.bookingRequest)
        assertEquals(AjastaWorkMode.PROD, context.workMode)
    }

    @Test
    fun `should exclude empty values from response`() {
        val context = AjastaContext(
            command = AjastaCommand.READ_BOOKING,
            requestId = AjastaRequestId(""),
            bookingResponse = AjastaBooking(
                id = AjastaBookingId("booking-123"),
                title = "",
                description = ""
            )
        )

        val response = context.toTransport() as BookingReadResponse

        assertNull(response.requestId)
        assertEquals("booking-123", response.booking?.id)
        assertNull(response.booking?.title)
        assertNull(response.booking?.description)
    }
}
