package top.ajasta.biz

import top.ajasta.biz.general.initStatus
import top.ajasta.biz.general.operation
import top.ajasta.biz.general.prepareResult
import top.ajasta.biz.repo.*
import top.ajasta.biz.stubs.*
import top.ajasta.biz.validation.*
import top.ajasta.common.AjastaContext
import top.ajasta.common.models.AjastaBookingId
import top.ajasta.common.models.AjastaResourceId
import top.ajasta.lib.cor.chain
import top.ajasta.lib.cor.rootChain
import top.ajasta.lib.cor.worker

class AjastaProcessor {

    suspend fun exec(ctx: AjastaContext) = businessChain.exec(ctx as BizContext)

    private val businessChain = rootChain<BizContext> {
        initStatus("Initializing status")

        // ==================== BOOKING OPERATIONS ====================
        operation("Create booking", top.ajasta.common.models.AjastaCommand.CREATE_BOOKING) {
            stubs("Processing stubs") {
                stubBookingCreateSuccess("Success case simulation")
                stubNotFoundError("Not found case simulation")
                stubValidationError("Validation error case simulation")
                stubDeleteError("Delete error case simulation")
                stubSearchError("Search error case simulation")
                stubNoCase("Error: requested stub is invalid")
            }
            validation {
                worker("Copy fields to bookingValidating") { bookingValidating = bookingRequest.deepCopy() }
                worker("Clear id") { bookingValidating.id = AjastaBookingId.NONE }
                worker("Trim title") { bookingValidating.title = bookingValidating.title.trim() }
                worker("Trim description") { bookingValidating.description = bookingValidating.description.trim() }
                validateBookingTitleNotEmpty("Check title is not empty")
                validateBookingTitleLength("Check title length")
                validateBookingDescriptionLength("Check description length")
                validateBookingResourceIdNotEmpty("Check resourceId is provided")
                validateSlotsNotEmpty("Check slots are provided")
                finishBookingValidation("Complete validation")
            }
            repoPrepareBookingCreate("Prepare booking for repo")
            repoBookingCreate("Create booking in DB")
            prepareResult("Prepare response")
        }

        operation("Read booking", top.ajasta.common.models.AjastaCommand.READ_BOOKING) {
            stubs("Processing stubs") {
                stubBookingReadSuccess("Success case simulation")
                stubNotFoundError("Not found case simulation")
                stubValidationError("Validation error case simulation")
                stubNoCase("Error: requested stub is invalid")
            }
            validation {
                worker("Copy fields to bookingValidating") { bookingValidating = bookingRequest.deepCopy() }
                worker("Trim id") { bookingValidating.id = AjastaBookingId(bookingValidating.id.asString().trim()) }
                validateBookingIdNotEmpty("Check id is not empty")
                validateBookingIdFormat("Check id format")
                finishBookingValidation("Complete validation")
            }
            repoBookingRead("Read booking from DB")
            prepareResult("Prepare response")
        }

        operation("Update booking", top.ajasta.common.models.AjastaCommand.UPDATE_BOOKING) {
            stubs("Processing stubs") {
                stubBookingUpdateSuccess("Success case simulation")
                stubNotFoundError("Not found case simulation")
                stubValidationError("Validation error case simulation")
                stubNoCase("Error: requested stub is invalid")
            }
            validation {
                worker("Copy fields to bookingValidating") { bookingValidating = bookingRequest.deepCopy() }
                worker("Trim id") { bookingValidating.id = AjastaBookingId(bookingValidating.id.asString().trim()) }
                worker("Trim title") { bookingValidating.title = bookingValidating.title.trim() }
                worker("Trim description") { bookingValidating.description = bookingValidating.description.trim() }
                validateBookingIdNotEmpty("Check id is not empty")
                validateBookingIdFormat("Check id format")
                validateBookingTitleNotEmpty("Check title is not empty")
                validateBookingTitleLength("Check title length")
                validateBookingDescriptionLength("Check description length")
                finishBookingValidation("Complete validation")
            }
            repoBookingRead("Read existing booking for lock")
            repoPrepareBookingUpdate("Prepare booking for update")
            repoBookingUpdate("Update booking in DB")
            prepareResult("Prepare response")
        }

        operation("Delete booking", top.ajasta.common.models.AjastaCommand.DELETE_BOOKING) {
            stubs("Processing stubs") {
                stubBookingDeleteSuccess("Success case simulation")
                stubNotFoundError("Not found case simulation")
                stubDeleteError("Delete error case simulation")
                stubNoCase("Error: requested stub is invalid")
            }
            validation {
                worker("Copy fields to bookingValidating") { bookingValidating = bookingRequest.deepCopy() }
                worker("Trim id") { bookingValidating.id = AjastaBookingId(bookingValidating.id.asString().trim()) }
                validateBookingIdNotEmpty("Check id is not empty")
                validateBookingIdFormat("Check id format")
                finishBookingValidation("Complete validation")
            }
            repoBookingRead("Read existing booking for lock")
            repoBookingDelete("Delete booking from DB")
            prepareResult("Prepare response")
        }

        operation("Search bookings", top.ajasta.common.models.AjastaCommand.SEARCH_BOOKINGS) {
            stubs("Processing stubs") {
                stubBookingSearchSuccess("Success case simulation")
                stubSearchError("Search error case simulation")
                stubNoCase("Error: requested stub is invalid")
            }
            validation {
                worker("Copy fields to bookingFilterValidating") { bookingFilterValidating = bookingFilterRequest }
                finishBookingFilterValidation("Complete validation")
            }
            repoBookingSearch("Search bookings in DB")
            prepareResult("Prepare response")
        }

        // ==================== RESOURCE OPERATIONS ====================
        operation("Create resource", top.ajasta.common.models.AjastaCommand.CREATE_RESOURCE) {
            stubs("Processing stubs") {
                stubResourceCreateSuccess("Success case simulation")
                stubValidationError("Validation error case simulation")
                stubNoCase("Error: requested stub is invalid")
            }
            validation {
                worker("Copy fields to resourceValidating") { resourceValidating = resourceRequest.deepCopy() }
                worker("Clear id") { resourceValidating.id = AjastaResourceId.NONE }
                worker("Trim name") { resourceValidating.name = resourceValidating.name.trim() }
                worker("Trim description") { resourceValidating.description = resourceValidating.description.trim() }
                validateResourceNameNotEmpty("Check name is not empty")
                validateResourceNameLength("Check name length")
                validateResourcePricePositive("Check price is positive")
                finishResourceValidation("Complete validation")
            }
            repoPrepareResourceCreate("Prepare resource for repo")
            repoResourceCreate("Create resource in DB")
            prepareResult("Prepare response")
        }

        operation("Read resource", top.ajasta.common.models.AjastaCommand.READ_RESOURCE) {
            stubs("Processing stubs") {
                stubResourceReadSuccess("Success case simulation")
                stubNotFoundError("Not found case simulation")
                stubNoCase("Error: requested stub is invalid")
            }
            validation {
                worker("Copy fields to resourceValidating") { resourceValidating = resourceRequest.deepCopy() }
                worker("Trim id") { resourceValidating.id = AjastaResourceId(resourceValidating.id.asString().trim()) }
                validateResourceIdNotEmpty("Check id is not empty")
                finishResourceValidation("Complete validation")
            }
            repoResourceRead("Read resource from DB")
            prepareResult("Prepare response")
        }

        operation("Update resource", top.ajasta.common.models.AjastaCommand.UPDATE_RESOURCE) {
            stubs("Processing stubs") {
                stubResourceUpdateSuccess("Success case simulation")
                stubNotFoundError("Not found case simulation")
                stubValidationError("Validation error case simulation")
                stubNoCase("Error: requested stub is invalid")
            }
            validation {
                worker("Copy fields to resourceValidating") { resourceValidating = resourceRequest.deepCopy() }
                worker("Trim id") { resourceValidating.id = AjastaResourceId(resourceValidating.id.asString().trim()) }
                worker("Trim name") { resourceValidating.name = resourceValidating.name.trim() }
                worker("Trim description") { resourceValidating.description = resourceValidating.description.trim() }
                validateResourceIdNotEmpty("Check id is not empty")
                validateResourceNameNotEmpty("Check name is not empty")
                validateResourceNameLength("Check name length")
                validateResourcePricePositive("Check price is positive")
                finishResourceValidation("Complete validation")
            }
            repoResourceRead("Read existing resource for lock")
            repoPrepareResourceUpdate("Prepare resource for update")
            repoResourceUpdate("Update resource in DB")
            prepareResult("Prepare response")
        }

        operation("Delete resource", top.ajasta.common.models.AjastaCommand.DELETE_RESOURCE) {
            stubs("Processing stubs") {
                stubResourceDeleteSuccess("Success case simulation")
                stubNotFoundError("Not found case simulation")
                stubNoCase("Error: requested stub is invalid")
            }
            validation {
                worker("Copy fields to resourceValidating") { resourceValidating = resourceRequest.deepCopy() }
                worker("Trim id") { resourceValidating.id = AjastaResourceId(resourceValidating.id.asString().trim()) }
                validateResourceIdNotEmpty("Check id is not empty")
                finishResourceValidation("Complete validation")
            }
            repoResourceRead("Read existing resource for lock")
            repoResourceDelete("Delete resource from DB")
            prepareResult("Prepare response")
        }

        operation("Search resources", top.ajasta.common.models.AjastaCommand.SEARCH_RESOURCES) {
            stubs("Processing stubs") {
                stubResourceSearchSuccess("Success case simulation")
                stubSearchError("Search error case simulation")
                stubNoCase("Error: requested stub is invalid")
            }
            validation {
                worker("Copy fields to resourceFilterValidating") { resourceFilterValidating = resourceFilterRequest }
                finishResourceFilterValidation("Complete validation")
            }
            repoResourceSearch("Search resources in DB")
            prepareResult("Prepare response")
        }

        operation("Get availability", top.ajasta.common.models.AjastaCommand.GET_AVAILABILITY) {
            stubs("Processing stubs") {
                stubGetAvailabilitySuccess("Success case simulation")
                stubNotFoundError("Not found case simulation")
                stubNoCase("Error: requested stub is invalid")
            }
            validation {
                validateAvailabilityResourceIdNotEmpty("Check resourceId is provided")
                validateAvailabilityDatesProvided("Check dates are provided")
                validateAvailabilityDateRange("Check date range is valid")
            }
            prepareResult("Prepare response")
        }
    }.build()
}
