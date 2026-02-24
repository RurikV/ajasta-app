package top.ajasta.repo.tests.resource

import top.ajasta.common.models.*
import top.ajasta.repo.DbResourceIdRequest
import top.ajasta.repo.DbResourceRequest
import top.ajasta.repo.IDbResourceResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for new availability fields in resource repository:
 * - active: Whether resource is available for booking
 * - unavailableWeekdays: CSV of weekday indices (0-6) when unavailable
 * - unavailableDates: CSV of dates (yyyy-MM-dd) when unavailable
 * - dailyUnavailableRanges: Semicolon-separated time ranges (HH:mm-HH:mm)
 */
abstract class RepoResourceAvailabilityFieldsTest {
    abstract val repo: top.ajasta.repo.IRepoResource

    private val activeResource by lazy { initObjects[0] }
    private val inactiveResource by lazy { initObjects[1] }
    private val resourceWithAvailabilityRules by lazy { initObjects[2] }

    @Test
    fun createResourceWithActiveTrue() = runRepoTest {
        val resource = AjastaResource(
            name = "Active Resource",
            type = AjastaResourceType.TURF_COURT,
            pricePerSlot = 50.0,
            active = true
        )

        val result = repo.createResource(DbResourceRequest(resource))
        assertIs<IDbResourceResponse.Ok>(result)
        assertTrue(result.data.active)
    }

    @Test
    fun createResourceWithActiveFalse() = runRepoTest {
        val resource = AjastaResource(
            name = "Inactive Resource",
            type = AjastaResourceType.VOLLEYBALL_COURT,
            pricePerSlot = 30.0,
            active = false
        )

        val result = repo.createResource(DbResourceRequest(resource))
        assertIs<IDbResourceResponse.Ok>(result)
        assertFalse(result.data.active)
    }

    @Test
    fun createResourceWithUnavailableWeekdays() = runRepoTest {
        val resource = AjastaResource(
            name = "Weekend Closed Resource",
            type = AjastaResourceType.PLAYGROUND,
            pricePerSlot = 20.0,
            unavailableWeekdays = "0,6" // Closed on Sundays and Saturdays
        )

        val result = repo.createResource(DbResourceRequest(resource))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals("0,6", result.data.unavailableWeekdays)
    }

    @Test
    fun createResourceWithUnavailableDates() = runRepoTest {
        val resource = AjastaResource(
            name = "Holiday Closed Resource",
            type = AjastaResourceType.HAIRDRESSING_CHAIR,
            pricePerSlot = 40.0,
            unavailableDates = "2025-01-01,2025-12-25,2025-07-04"
        )

        val result = repo.createResource(DbResourceRequest(resource))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals("2025-01-01,2025-12-25,2025-07-04", result.data.unavailableDates)
    }

    @Test
    fun createResourceWithDailyUnavailableRanges() = runRepoTest {
        val resource = AjastaResource(
            name = "Lunch Break Resource",
            type = AjastaResourceType.OTHER,
            pricePerSlot = 25.0,
            dailyUnavailableRanges = "12:00-13:00;17:00-18:00"
        )

        val result = repo.createResource(DbResourceRequest(resource))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals("12:00-13:00;17:00-18:00", result.data.dailyUnavailableRanges)
    }

    @Test
    fun createResourceWithAllAvailabilityFields() = runRepoTest {
        val resource = AjastaResource(
            name = "Full Availability Config Resource",
            type = AjastaResourceType.TURF_COURT,
            pricePerSlot = 60.0,
            active = false,
            unavailableWeekdays = "1", // Closed on Mondays
            unavailableDates = "2025-01-01,2025-12-25",
            dailyUnavailableRanges = "14:00-15:00"
        )

        val result = repo.createResource(DbResourceRequest(resource))
        assertIs<IDbResourceResponse.Ok>(result)
        assertFalse(result.data.active)
        assertEquals("1", result.data.unavailableWeekdays)
        assertEquals("2025-01-01,2025-12-25", result.data.unavailableDates)
        assertEquals("14:00-15:00", result.data.dailyUnavailableRanges)
    }

    @Test
    fun readResourceWithAvailabilityFields() = runRepoTest {
        val result = repo.readResource(DbResourceIdRequest(resourceWithAvailabilityRules))
        assertIs<IDbResourceResponse.Ok>(result)
        assertFalse(result.data.active)
        assertEquals("0,6", result.data.unavailableWeekdays)
        assertEquals("2025-01-01,2025-12-25", result.data.unavailableDates)
        assertEquals("12:00-13:00", result.data.dailyUnavailableRanges)
    }

    @Test
    fun updateResourceActiveStatus() = runRepoTest {
        val updatedResource = activeResource.copy(
            active = false,
            lock = activeResource.lock
        )

        val result = repo.updateResource(DbResourceRequest(updatedResource))
        assertIs<IDbResourceResponse.Ok>(result)
        assertFalse(result.data.active)
    }

    @Test
    fun updateResourceAvailabilityRules() = runRepoTest {
        val updatedResource = activeResource.copy(
            unavailableWeekdays = "0",
            unavailableDates = "2025-07-04",
            dailyUnavailableRanges = "13:00-14:00",
            lock = activeResource.lock
        )

        val result = repo.updateResource(DbResourceRequest(updatedResource))
        assertIs<IDbResourceResponse.Ok>(result)
        assertEquals("0", result.data.unavailableWeekdays)
        assertEquals("2025-07-04", result.data.unavailableDates)
        assertEquals("13:00-14:00", result.data.dailyUnavailableRanges)
    }

    @Test
    fun updateResourceClearAvailabilityRules() = runRepoTest {
        val updatedResource = resourceWithAvailabilityRules.copy(
            active = true,
            unavailableWeekdays = "",
            unavailableDates = "",
            dailyUnavailableRanges = "",
            lock = resourceWithAvailabilityRules.lock
        )

        val result = repo.updateResource(DbResourceRequest(updatedResource))
        assertIs<IDbResourceResponse.Ok>(result)
        assertTrue(result.data.active)
        assertEquals("", result.data.unavailableWeekdays)
        assertEquals("", result.data.unavailableDates)
        assertEquals("", result.data.dailyUnavailableRanges)
    }

    @Test
    fun searchResourcesReturnsAvailabilityFields() = runRepoTest {
        val filter = top.ajasta.repo.DbResourceFilterRequest()

        val result = repo.searchResources(filter)
        assertIs<top.ajasta.repo.IDbResourcesResponse.Ok>(result)

        // Find our test resource with availability rules
        val found = result.data.find { it.id == resourceWithAvailabilityRules.id }
        assert(found != null)
        assertEquals("0,6", found!!.unavailableWeekdays)
        assertEquals("2025-01-01,2025-12-25", found.unavailableDates)
        assertEquals("12:00-13:00", found.dailyUnavailableRanges)
    }

    companion object : BaseInitResource("availability") {
        override val initObjects: List<AjastaResource> = listOf(
            // Active resource with no restrictions
            createInitTestModel(
                suf = "active",
                active = true,
                unavailableWeekdays = "",
                unavailableDates = "",
                dailyUnavailableRanges = ""
            ),
            // Inactive resource
            createInitTestModel(
                suf = "inactive",
                active = false,
                unavailableWeekdays = "",
                unavailableDates = "",
                dailyUnavailableRanges = ""
            ),
            // Resource with all availability rules
            createInitTestModel(
                suf = "with-rules",
                active = false,
                unavailableWeekdays = "0,6",
                unavailableDates = "2025-01-01,2025-12-25",
                dailyUnavailableRanges = "12:00-13:00"
            )
        )
    }
}
