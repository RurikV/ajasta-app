package top.ajasta.biz

import kotlinx.coroutines.test.runTest
import top.ajasta.biz.BizContext
import top.ajasta.common.models.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookingStubTest {

    private val processor = AjastaProcessor()

    @Test
    fun createBookingSuccess() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.SUCCESS
            bookingRequest = AjastaBooking(
                title = "Test Booking",
                description = "Test Description",
                resourceId = AjastaResourceId("resource-123")
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FINISHING, ctx.state)
        assertTrue(ctx.bookingResponse.title.isNotEmpty())
        assertTrue(ctx.paymentLink.isNotEmpty())
    }

    @Test
    fun createBookingValidationError() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.VALIDATION_ERROR
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.isNotEmpty())
    }

    @Test
    fun readBookingSuccess() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.READ_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.SUCCESS
            bookingRequest = AjastaBooking(
                id = AjastaBookingId("booking-123")
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FINISHING, ctx.state)
        assertTrue(ctx.bookingResponse.id != AjastaBookingId.NONE)
    }

    @Test
    fun readBookingNotFound() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.READ_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.NOT_FOUND
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.code == "not-found" })
    }

    @Test
    fun updateBookingSuccess() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.UPDATE_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.SUCCESS
            bookingRequest = AjastaBooking(
                id = AjastaBookingId("booking-123"),
                title = "Updated Title",
                description = "Updated Description"
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FINISHING, ctx.state)
        assertEquals("Updated Title", ctx.bookingResponse.title)
    }

    @Test
    fun deleteBookingSuccess() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.DELETE_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.SUCCESS
            bookingRequest = AjastaBooking(
                id = AjastaBookingId("booking-123")
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FINISHING, ctx.state)
        assertEquals(AjastaBookingStatus.CANCELLED, ctx.bookingResponse.bookingStatus)
        assertTrue(ctx.refundAmount > 0)
    }

    @Test
    fun deleteBookingError() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.DELETE_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.DELETE_ERROR
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.code == "delete-error" })
    }

    @Test
    fun searchBookingsSuccess() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.SEARCH_BOOKINGS
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.SUCCESS
            bookingFilterRequest = AjastaBookingFilter()
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FINISHING, ctx.state)
        assertTrue(ctx.bookingsResponse.isNotEmpty())
    }

    @Test
    fun searchBookingsError() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.SEARCH_BOOKINGS
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.SEARCH_ERROR
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.code == "search-error" })
    }
}
