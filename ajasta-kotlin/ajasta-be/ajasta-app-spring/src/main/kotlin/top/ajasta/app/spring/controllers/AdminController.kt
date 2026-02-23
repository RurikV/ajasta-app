package top.ajasta.app.spring.controllers

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import top.ajasta.common.models.*
import top.ajasta.repo.DbBookingRequest
import top.ajasta.repo.DbResourceRequest
import top.ajasta.repo.IRepoBooking
import top.ajasta.repo.IRepoResource
import java.util.*

/**
 * Admin controller for testing utilities.
 * Provides endpoints for bulk data generation to facilitate manual testing.
 */
@RestController
@RequestMapping("admin")
class AdminController(
    private val repoResource: IRepoResource,
    private val repoBooking: IRepoBooking
) {
    private val logger = LoggerFactory.getLogger(AdminController::class.java)

    /**
     * Generates bulk test data for resources and optionally bookings.
     *
     * @param resourceCount Number of resources to generate (default: 10, max: 100)
     * @param bookingPerResource Number of bookings per resource (default: 0, max: 10)
     * @param ownerId Optional owner ID for all resources
     * @return Summary of generated data
     */
    @PostMapping("generate-test-data")
    suspend fun generateTestData(
        @RequestParam(defaultValue = "10") resourceCount: Int,
        @RequestParam(defaultValue = "0") bookingPerResource: Int,
        @RequestParam(required = false) ownerId: String?
    ): TestDataGenerationResponse {
        // Validate parameters
        val validResourceCount = resourceCount.coerceIn(1, 100)
        val validBookingCount = bookingPerResource.coerceIn(0, 10)

        logger.info("Generating test data: $validResourceCount resources, $validBookingCount bookings per resource")

        val generatedResources = mutableListOf<AjastaResource>()
        val generatedBookings = mutableListOf<AjastaBooking>()
        val errors = mutableListOf<String>()
        val now = Clock.System.now()

        try {
            // Generate resources
            val resources = generateResources(validResourceCount, ownerId, now)
            resources.forEach { resource ->
                when (val result = repoResource.createResource(DbResourceRequest(resource))) {
                    is top.ajasta.repo.IDbResourceResponse.Ok -> {
                        generatedResources.add(result.data)
                        logger.debug("Created resource: ${result.data.id}")
                    }
                    is top.ajasta.repo.IDbResourceResponse.Err -> {
                        errors.add("Failed to create resource: ${result.errors.joinToString()}")
                    }
                    else -> {
                        errors.add("Unexpected response creating resource")
                    }
                }
            }

            // Generate bookings for each resource
            if (validBookingCount > 0) {
                generatedResources.forEach { resource ->
                    val bookings = generateBookingsForResource(resource, validBookingCount, now)
                    bookings.forEach { booking ->
                        when (val result = repoBooking.createBooking(DbBookingRequest(booking))) {
                            is top.ajasta.repo.IDbBookingResponse.Ok -> {
                                generatedBookings.add(result.data)
                                logger.debug("Created booking: ${result.data.id}")
                            }
                            is top.ajasta.repo.IDbBookingResponse.Err -> {
                                errors.add("Failed to create booking: ${result.errors.joinToString()}")
                            }
                            else -> {
                                errors.add("Unexpected response creating booking")
                            }
                        }
                    }
                }
            }

            return TestDataGenerationResponse(
                success = errors.isEmpty(),
                resourcesCreated = generatedResources.size,
                bookingsCreated = generatedBookings.size,
                resourceIds = generatedResources.map { it.id.asString() },
                bookingIds = generatedBookings.map { it.id.asString() },
                errors = errors.takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            logger.error("Error generating test data", e)
            return TestDataGenerationResponse(
                success = false,
                resourcesCreated = generatedResources.size,
                bookingsCreated = generatedBookings.size,
                resourceIds = generatedResources.map { it.id.asString() },
                bookingIds = generatedBookings.map { it.id.asString() },
                errors = listOf("Exception: ${e.message}")
            )
        }
    }

    /**
     * Clears all test data from the database.
     * Use with caution - this will delete all resources and bookings.
     */
    @DeleteMapping("clear-test-data")
    suspend fun clearTestData(): ClearDataResponse {
        logger.warn("Clearing all test data")
        var resourcesDeleted = 0
        var bookingsDeleted = 0
        val errors = mutableListOf<String>()

        try {
            // Search all resources
            val allResources = repoResource.searchResources(
                top.ajasta.repo.DbResourceFilterRequest()
            )

            when (allResources) {
                is top.ajasta.repo.IDbResourcesResponse.Ok -> {
                    allResources.data.forEach { resource ->
                        when (repoResource.deleteResource(top.ajasta.repo.DbResourceIdRequest(resource.id, resource.lock))) {
                            is top.ajasta.repo.IDbResourceResponse.Ok -> resourcesDeleted++
                            else -> errors.add("Failed to delete resource ${resource.id}")
                        }
                    }
                }
                else -> errors.add("Failed to search resources")
            }

            return ClearDataResponse(
                success = errors.isEmpty(),
                resourcesDeleted = resourcesDeleted,
                bookingsDeleted = bookingsDeleted,
                errors = errors.takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            logger.error("Error clearing test data", e)
            return ClearDataResponse(
                success = false,
                resourcesDeleted = resourcesDeleted,
                bookingsDeleted = bookingsDeleted,
                errors = listOf("Exception: ${e.message}")
            )
        }
    }

    private fun generateResources(count: Int, ownerId: String?, now: Instant): List<AjastaResource> {
        val resourceTypes = AjastaResourceType.entries.filter { it != AjastaResourceType.NONE }
        val locations = listOf(
            "Downtown Sports Center",
            "Northside Complex",
            "Central Park Facility",
            "Eastside Arena",
            "Westend Recreation Center",
            "Harbor View Courts",
            "Mountain View Playground",
            "Lakeside Sports Hub",
            "University Campus",
            "Community Center"
        )
        val baseNames = mapOf(
            AjastaResourceType.TURF_COURT to listOf("Tennis Court", "Football Field", "Soccer Pitch", "Rugby Ground"),
            AjastaResourceType.VOLLEYBALL_COURT to listOf("Volleyball Court", "Beach Court", "Indoor Court"),
            AjastaResourceType.PLAYGROUND to listOf("Kids Playground", "Adventure Zone", "Fun Park"),
            AjastaResourceType.HAIRDRESSING_CHAIR to listOf("Styling Station", "Hair Station", "Barber Chair"),
            AjastaResourceType.OTHER to listOf("Multi-purpose Room", "Event Space", "Activity Hall")
        )

        return (1..count).map { index ->
            val type = resourceTypes.random()
            val baseName = baseNames[type]?.random() ?: "Resource"
            val location = locations.random()

            AjastaResource(
                id = AjastaResourceId(UUID.randomUUID().toString()),
                name = "$baseName $index",
                type = type,
                location = "$location, Building ${(index % 5) + 1}",
                description = "Test resource $index - A ${type.name.lowercase().replace("_", " ")} for testing purposes",
                imageUrl = "https://picsum.photos/seed/$index/400/300",
                pricePerSlot = (10.0 + (index * 2.5)).coerceAtMost(100.0),
                unitsCount = (1..5).random(),
                openTime = "0${(6..9).random()}:00",
                closeTime = "${(18..22).random()}:00",
                rating = (3.0 + (index % 5) * 0.5).coerceAtMost(5.0),
                reviewCount = (10..100).random(),
                ownerId = AjastaUserId(ownerId ?: "test-owner-${index % 3}"),
                active = index % 10 != 0, // 90% active
                unavailableWeekdays = generateUnavailableWeekdays(index),
                unavailableDates = generateUnavailableDates(index),
                dailyUnavailableRanges = generateDailyUnavailableRanges(index),
                lock = AjastaLock(UUID.randomUUID().toString()),
                createdAt = now,
                updatedAt = now
            )
        }
    }

    private fun generateUnavailableWeekdays(index: Int): String {
        return when (index % 4) {
            0 -> "" // Always available
            1 -> "0" // Closed on Sundays
            2 -> "0,6" // Closed on weekends
            3 -> "1" // Closed on Mondays (maintenance)
            else -> ""
        }
    }

    private fun generateUnavailableDates(index: Int): String {
        return when (index % 3) {
            0 -> ""
            1 -> {
                // Some holidays
                val year = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() / 31536000000 + 1970
                "$year-01-01,$year-12-25,$year-07-04"
            }
            2 -> {
                val year = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() / 31536000000 + 1970
                "$year-01-01,$year-12-31"
            }
            else -> ""
        }
    }

    private fun generateDailyUnavailableRanges(index: Int): String {
        return when (index % 4) {
            0 -> ""
            1 -> "12:00-13:00" // Lunch break
            2 -> "12:00-13:00;17:00-18:00" // Lunch and dinner break
            3 -> "14:00-15:00" // Maintenance window
            else -> ""
        }
    }

    private fun generateBookingsForResource(resource: AjastaResource, count: Int, now: Instant): List<AjastaBooking> {
        // Use fixed test dates for simplicity - these are just test bookings
        val baseDate = now.toString().substringBefore("T")
        return (1..count).map { index ->
            val startHour = 9 + index
            AjastaBooking(
                id = AjastaBookingId(UUID.randomUUID().toString()),
                title = "Test Booking $index for ${resource.name}",
                description = "Test booking created by admin generator",
                resourceId = resource.id,
                userId = AjastaUserId("test-user-${index % 5}"),
                bookingStatus = AjastaBookingStatus.entries.filter { it != AjastaBookingStatus.NONE }.random(),
                paymentStatus = AjastaPaymentStatus.entries.filter { it != AjastaPaymentStatus.NONE }.random(),
                slots = listOf(
                    AjastaSlot(
                        slotStart = kotlinx.datetime.Instant.parse("${baseDate}T${startHour.toString().padStart(2, '0')}:00:00Z"),
                        slotEnd = kotlinx.datetime.Instant.parse("${baseDate}T${(startHour + 1).toString().padStart(2, '0')}:00:00Z"),
                        price = resource.pricePerSlot
                    )
                ),
                lock = AjastaLock(UUID.randomUUID().toString()),
                createdAt = now,
                updatedAt = now
            )
        }
    }
}

data class TestDataGenerationResponse(
    val success: Boolean,
    val resourcesCreated: Int,
    val bookingsCreated: Int,
    val resourceIds: List<String>,
    val bookingIds: List<String>,
    val errors: List<String>?
)

data class ClearDataResponse(
    val success: Boolean,
    val resourcesDeleted: Int,
    val bookingsDeleted: Int,
    val errors: List<String>?
)
