package top.ajasta.app.common

import top.ajasta.biz.BizContext
import top.ajasta.common.models.*
import top.ajasta.stubs.AjastaBookingStubs
import top.ajasta.stubs.AjastaResourceStubs

/**
 * Stub processor that returns fake data for testing and development.
 */
class AjastaStubProcessor : AjastaProcessor {

    override suspend fun exec(ctx: BizContext) {
        when (ctx.workMode) {
            AjastaWorkMode.STUB -> processStub(ctx)
            else -> processStub(ctx) // For now, all modes use stubs
        }
    }

    private fun processStub(ctx: BizContext) {
        when (ctx.stubCase) {
            AjastaStubs.SUCCESS -> processSuccess(ctx)
            AjastaStubs.NOT_FOUND -> processNotFound(ctx)
            AjastaStubs.VALIDATION_ERROR -> processValidationError(ctx)
            AjastaStubs.DELETE_ERROR -> processDeleteError(ctx)
            AjastaStubs.SEARCH_ERROR -> processSearchError(ctx)
            AjastaStubs.NONE -> processSuccess(ctx)
        }
    }

    private fun processSuccess(ctx: BizContext) {
        when (ctx.command) {
            AjastaCommand.CREATE_BOOKING -> {
                ctx.bookingResponse = AjastaBookingStubs.BOOKING_TENNIS
                ctx.paymentLink = "https://payment.example.com/pay/stub-123"
                ctx.state = AjastaState.FINISHING
            }
            AjastaCommand.READ_BOOKING -> {
                ctx.bookingResponse = AjastaBookingStubs.BOOKING_TENNIS
                ctx.state = AjastaState.FINISHING
            }
            AjastaCommand.UPDATE_BOOKING -> {
                val existing = AjastaBookingStubs.BOOKING_TENNIS
                ctx.bookingResponse = existing.copy(
                    title = ctx.bookingRequest.title.ifEmpty { existing.title },
                    description = ctx.bookingRequest.description.ifEmpty { existing.description }
                )
                ctx.state = AjastaState.FINISHING
            }
            AjastaCommand.DELETE_BOOKING -> {
                ctx.bookingResponse = AjastaBooking(
                    id = ctx.bookingRequest.id,
                    bookingStatus = AjastaBookingStatus.CANCELLED
                )
                ctx.refundAmount = 25.0
                ctx.state = AjastaState.FINISHING
            }
            AjastaCommand.SEARCH_BOOKINGS -> {
                val filterResourceId = ctx.bookingFilterRequest.resourceId.asString()
                val results = listOfNotNull(
                    AjastaBookingStubs.BOOKING_TENNIS.takeIf {
                        filterResourceId.isEmpty() || it.resourceId.asString() == filterResourceId
                    },
                    AjastaBookingStubs.BOOKING_HAIRDRESSING.takeIf {
                        filterResourceId.isEmpty() || it.resourceId.asString() == filterResourceId
                    }
                )
                ctx.bookingsResponse.addAll(results)
                ctx.total = ctx.bookingsResponse.size
                ctx.state = AjastaState.FINISHING
            }
            AjastaCommand.CREATE_RESOURCE -> {
                ctx.resourceResponse = AjastaResourceStubs.RESOURCE_TENNIS_COURT
                ctx.state = AjastaState.FINISHING
            }
            AjastaCommand.READ_RESOURCE -> {
                ctx.resourceResponse = AjastaResourceStubs.RESOURCE_TENNIS_COURT
                ctx.state = AjastaState.FINISHING
            }
            AjastaCommand.UPDATE_RESOURCE -> {
                val existing = AjastaResourceStubs.RESOURCE_TENNIS_COURT
                ctx.resourceResponse = existing.copy(
                    name = ctx.resourceRequest.name.ifEmpty { existing.name }
                )
                ctx.state = AjastaState.FINISHING
            }
            AjastaCommand.DELETE_RESOURCE -> {
                ctx.resourceResponse = AjastaResource(id = ctx.resourceRequest.id)
                ctx.state = AjastaState.FINISHING
            }
            AjastaCommand.SEARCH_RESOURCES -> {
                val filterType = ctx.resourceFilterRequest.type
                val results = listOfNotNull(
                    AjastaResourceStubs.RESOURCE_TENNIS_COURT.takeIf {
                        filterType == AjastaResourceType.NONE || it.type == filterType
                    },
                    AjastaResourceStubs.RESOURCE_VOLLEYBALL_COURT.takeIf {
                        filterType == AjastaResourceType.NONE || it.type == filterType
                    },
                    AjastaResourceStubs.RESOURCE_HAIRDRESSING.takeIf {
                        filterType == AjastaResourceType.NONE || it.type == filterType
                    },
                    AjastaResourceStubs.RESOURCE_PLAYGROUND.takeIf {
                        filterType == AjastaResourceType.NONE || it.type == filterType
                    }
                )
                ctx.resourcesResponse.addAll(results)
                ctx.total = ctx.resourcesResponse.size
                ctx.state = AjastaState.FINISHING
            }
            AjastaCommand.GET_AVAILABILITY -> {
                val slots = listOf(
                    AjastaSlot(
                        slotStart = kotlinx.datetime.Instant.parse("2025-03-01T10:00:00Z"),
                        slotEnd = kotlinx.datetime.Instant.parse("2025-03-01T11:00:00Z"),
                        price = 30.0
                    ),
                    AjastaSlot(
                        slotStart = kotlinx.datetime.Instant.parse("2025-03-01T11:00:00Z"),
                        slotEnd = kotlinx.datetime.Instant.parse("2025-03-01T12:00:00Z"),
                        price = 30.0
                    ),
                    AjastaSlot(
                        slotStart = kotlinx.datetime.Instant.parse("2025-03-01T14:00:00Z"),
                        slotEnd = kotlinx.datetime.Instant.parse("2025-03-01T15:00:00Z"),
                        price = 25.0
                    )
                )
                ctx.availableSlots.addAll(slots)
                ctx.state = AjastaState.FINISHING
            }
            AjastaCommand.NONE -> {
                ctx.state = AjastaState.FAILING
                ctx.errors.add(AjastaError(code = "no-command", message = "No command specified"))
            }
        }
    }

    private fun processNotFound(ctx: BizContext) {
        ctx.state = AjastaState.FAILING
        ctx.errors.add(AjastaError(code = "not-found", group = "stub", message = "Not found"))
    }

    private fun processValidationError(ctx: BizContext) {
        ctx.state = AjastaState.FAILING
        ctx.errors.add(AjastaError(code = "validation-error", group = "stub", field = "data", message = "Validation error"))
    }

    private fun processDeleteError(ctx: BizContext) {
        ctx.state = AjastaState.FAILING
        ctx.errors.add(AjastaError(code = "delete-error", group = "stub", message = "Cannot delete"))
    }

    private fun processSearchError(ctx: BizContext) {
        ctx.state = AjastaState.FAILING
        ctx.errors.add(AjastaError(code = "search-error", group = "stub", message = "Search failed"))
    }
}
