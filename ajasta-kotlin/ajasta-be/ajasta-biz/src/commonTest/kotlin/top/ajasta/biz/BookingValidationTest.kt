package top.ajasta.biz

import kotlinx.coroutines.test.runTest
import top.ajasta.biz.BizContext
import top.ajasta.common.models.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookingValidationTest {

    private val processor = AjastaProcessor()

    @Test
    fun createBookingEmptyTitle() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            bookingRequest = AjastaBooking(
                title = "",
                description = "Test Description",
                resourceId = AjastaResourceId("resource-123"),
                slots = listOf(AjastaSlot())
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.field == "title" })
    }

    @Test
    fun createBookingTitleTooLong() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            bookingRequest = AjastaBooking(
                title = "x".repeat(101),
                description = "Test Description",
                resourceId = AjastaResourceId("resource-123"),
                slots = listOf(AjastaSlot())
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.code == "validation-title-length" })
    }

    @Test
    fun createBookingDescriptionTooLong() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            bookingRequest = AjastaBooking(
                title = "Test Title",
                description = "x".repeat(501),
                resourceId = AjastaResourceId("resource-123"),
                slots = listOf(AjastaSlot())
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.code == "validation-description-length" })
    }

    @Test
    fun createBookingMissingResourceId() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            bookingRequest = AjastaBooking(
                title = "Test Title",
                description = "Test Description",
                resourceId = AjastaResourceId.NONE,
                slots = listOf(AjastaSlot())
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.field == "resourceId" })
    }

    @Test
    fun createBookingEmptySlots() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            bookingRequest = AjastaBooking(
                title = "Test Title",
                description = "Test Description",
                resourceId = AjastaResourceId("resource-123"),
                slots = emptyList()
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.field == "slots" })
    }

    @Test
    fun readBookingEmptyId() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.READ_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            bookingRequest = AjastaBooking(
                id = AjastaBookingId.NONE
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.field == "id" })
    }

    @Test
    fun readBookingInvalidIdFormat() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.READ_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            bookingRequest = AjastaBooking(
                id = AjastaBookingId("invalid id!@#")
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.code == "validation-id-format" })
    }

    @Test
    fun updateBookingEmptyId() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.UPDATE_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            bookingRequest = AjastaBooking(
                id = AjastaBookingId.NONE,
                title = "Test Title"
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.field == "id" })
    }

    @Test
    fun deleteBookingEmptyId() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.DELETE_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            bookingRequest = AjastaBooking(
                id = AjastaBookingId.NONE
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.field == "id" })
    }

    @Test
    fun createBookingTrimTitle() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_BOOKING
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            bookingRequest = AjastaBooking(
                title = "  Test Title  ",
                description = "Test Description",
                resourceId = AjastaResourceId("resource-123"),
                slots = listOf(AjastaSlot())
            )
        }
        processor.exec(ctx)
        // The validation should pass, but there's no repo so it won't finish
        // Just checking that title trimming didn't cause validation errors
        assertTrue(ctx.errors.none { it.field == "title" && it.code == "validation-title-empty" })
    }
}
