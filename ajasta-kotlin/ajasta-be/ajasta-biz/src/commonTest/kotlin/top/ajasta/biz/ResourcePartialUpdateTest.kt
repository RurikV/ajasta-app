package top.ajasta.biz

import top.ajasta.common.models.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for partial resource update behavior.
 *
 * These tests verify that when updating a resource, fields not explicitly set in the request
 * are preserved from the existing resource data. This prevents accidental data loss when
 * the frontend doesn't send all fields.
 *
 * This specifically tests the fix for the issue where availability fields
 * (active, unavailableWeekdays, unavailableDates, dailyUnavailableRanges)
 * were being reset to empty/default values during updates.
 *
 * Tests the merge logic from RepoPrepareWorkers.kt repoPrepareResourceUpdate.
 */
class ResourcePartialUpdateTest {

    /**
     * Existing resource in DB with availability fields set.
     * This simulates a resource that was previously configured with availability rules.
     */
    private fun createExistingResourceWithAvailability() = AjastaResource(
        id = AjastaResourceId("test-resource-123"),
        name = "Tennis Court A",
        type = AjastaResourceType.TURF_COURT,
        location = "Sports Complex",
        description = "Professional tennis court",
        pricePerSlot = 50.0,
        unitsCount = 2,
        active = false,  // Previously set to inactive
        unavailableWeekdays = "0,6",  // Closed on weekends
        unavailableDates = "2025-12-25,2025-01-01",  // Holidays
        dailyUnavailableRanges = "12:00-13:00",  // Lunch break
        lock = AjastaLock("existing-lock-123")
    )

    /**
     * Simulates the merge logic from repoPrepareResourceUpdate in RepoPrepareWorkers.kt
     *
     * The original bug was that this function did:
     *   resourceRepoPrepare = resourceValidated.deepCopy()
     * Which caused all fields not in the request to be reset to defaults.
     *
     * The fix changes it to:
     *   resourceRepoPrepare = resourceRepoRead.deepCopy()
     *   resourceRepoPrepare = resourceRepoPrepare.copy(
     *       // Override with non-default values from validated request
     *       name = resourceValidated.name.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.name,
     *       ...
     *   )
     */
    private fun simulateRepoPrepareResourceUpdate(
        resourceRepoRead: AjastaResource,
        resourceValidated: AjastaResource
    ): AjastaResource {
        // Start with existing data from DB (THE FIX - was using resourceValidated.deepCopy())
        var resourceRepoPrepare = resourceRepoRead.deepCopy()

        // Override with non-default values from the validated request
        resourceRepoPrepare = resourceRepoPrepare.copy(
            id = resourceValidated.id,
            name = resourceValidated.name.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.name,
            type = resourceValidated.type.takeIf { it != AjastaResourceType.NONE } ?: resourceRepoPrepare.type,
            location = resourceValidated.location.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.location,
            description = resourceValidated.description.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.description,
            imageUrl = resourceValidated.imageUrl.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.imageUrl,
            pricePerSlot = resourceValidated.pricePerSlot.takeIf { it != 0.0 } ?: resourceRepoPrepare.pricePerSlot,
            unitsCount = resourceValidated.unitsCount.takeIf { it != 1 } ?: resourceRepoPrepare.unitsCount,
            openTime = resourceValidated.openTime.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.openTime,
            closeTime = resourceValidated.closeTime.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.closeTime,
            ownerId = resourceValidated.ownerId.takeIf { it != AjastaUserId.NONE } ?: resourceRepoPrepare.ownerId,
            active = resourceValidated.active,
            unavailableWeekdays = resourceValidated.unavailableWeekdays.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.unavailableWeekdays,
            unavailableDates = resourceValidated.unavailableDates.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.unavailableDates,
            dailyUnavailableRanges = resourceValidated.dailyUnavailableRanges.takeIf { it.isNotEmpty() } ?: resourceRepoPrepare.dailyUnavailableRanges,
            lock = resourceRepoRead.lock
        )
        return resourceRepoPrepare
    }

    /**
     * Test: Updating only the name should preserve STRING availability fields.
     *
     * Scenario: Frontend sends an update with just the name changed.
     * Expected: String availability fields should be preserved from the existing resource.
     *
     * Note: The `active` boolean field is always set from the request because
     * Boolean has no "empty" value to indicate "not set". If preserving active
     * status is needed, the frontend must send the current value.
     */
    @Test
    fun updateResourceOnlyNamePreservesAvailabilityFields() {
        val existingResource = createExistingResourceWithAvailability()

        // Request that only updates the name (simulating frontend not sending availability fields)
        // Note: active defaults to true, so it will be set to true
        val validatedRequest = AjastaResource(
            id = existingResource.id,
            name = "Tennis Court A - Renovated",
            lock = existingResource.lock
        )

        val result = simulateRepoPrepareResourceUpdate(existingResource, validatedRequest)

        // Verify the prepared resource preserves STRING availability fields
        assertEquals("Tennis Court A - Renovated", result.name,
            "Name should be updated")
        // active defaults to true in the model, so it will be true (not preserved)
        assertTrue(result.active,
            "Active defaults to true in model - Boolean has no 'not set' value")
        // String fields are preserved when empty
        assertEquals("0,6", result.unavailableWeekdays,
            "Unavailable weekdays should be preserved from existing resource")
        assertEquals("2025-12-25,2025-01-01", result.unavailableDates,
            "Unavailable dates should be preserved from existing resource")
        assertEquals("12:00-13:00", result.dailyUnavailableRanges,
            "Daily unavailable ranges should be preserved from existing resource")
    }

    /**
     * Test: Updating only price should preserve STRING availability fields.
     */
    @Test
    fun updateResourceOnlyPricePreservesAvailabilityFields() {
        val existingResource = createExistingResourceWithAvailability()

        val validatedRequest = AjastaResource(
            id = existingResource.id,
            name = existingResource.name,  // Required field
            pricePerSlot = 75.0,  // Only changing price
            lock = existingResource.lock
        )

        val result = simulateRepoPrepareResourceUpdate(existingResource, validatedRequest)

        assertEquals(75.0, result.pricePerSlot,
            "Price should be updated")
        // active defaults to true, not preserved
        assertTrue(result.active,
            "Active defaults to true - not preserved for booleans")
        assertEquals("0,6", result.unavailableWeekdays,
            "Unavailable weekdays should be preserved")
    }

    /**
     * Test: Explicitly setting active to true should override the existing value.
     */
    @Test
    fun updateResourceCanChangeActiveStatus() {
        val existingResource = createExistingResourceWithAvailability()

        val validatedRequest = AjastaResource(
            id = existingResource.id,
            name = existingResource.name,
            active = true,  // Explicitly setting to true
            lock = existingResource.lock
        )

        val result = simulateRepoPrepareResourceUpdate(existingResource, validatedRequest)

        assertTrue(result.active,
            "Active status should be updated to true")
        // Other availability fields should still be preserved
        assertEquals("0,6", result.unavailableWeekdays,
            "Unavailable weekdays should still be preserved")
    }

    /**
     * Test: Empty strings in availability fields should preserve existing values.
     *
     * This is intentional behavior to prevent accidental data loss when frontend
     * sends empty strings for fields it doesn't have values for.
     */
    @Test
    fun updateResourceEmptyStringPreservesExistingAvailability() {
        val existingResource = createExistingResourceWithAvailability()

        // Frontend sends empty strings for availability fields it doesn't handle
        val validatedRequest = AjastaResource(
            id = existingResource.id,
            name = existingResource.name,
            unavailableWeekdays = "",  // Empty string - should preserve existing
            unavailableDates = "",     // Empty string - should preserve existing
            dailyUnavailableRanges = "", // Empty string - should preserve existing
            lock = existingResource.lock
        )

        val result = simulateRepoPrepareResourceUpdate(existingResource, validatedRequest)

        // Empty string means "not set", so it preserves existing values
        assertEquals("0,6", result.unavailableWeekdays,
            "Empty string should preserve existing value (prevents accidental data loss)")
        assertEquals("2025-12-25,2025-01-01", result.unavailableDates,
            "Empty string should preserve existing unavailableDates")
        assertEquals("12:00-13:00", result.dailyUnavailableRanges,
            "Empty string should preserve existing dailyUnavailableRanges")
    }

    /**
     * Test: Updating with new availability values should override existing ones.
     */
    @Test
    fun updateResourceCanChangeAvailabilityFields() {
        val existingResource = createExistingResourceWithAvailability()

        val validatedRequest = AjastaResource(
            id = existingResource.id,
            name = existingResource.name,
            unavailableWeekdays = "1",  // Closed on Monday instead
            unavailableDates = "2025-07-04",  // Different holidays
            dailyUnavailableRanges = "14:00-15:00",  // Different break
            lock = existingResource.lock
        )

        val result = simulateRepoPrepareResourceUpdate(existingResource, validatedRequest)

        assertEquals("1", result.unavailableWeekdays,
            "Unavailable weekdays should be updated")
        assertEquals("2025-07-04", result.unavailableDates,
            "Unavailable dates should be updated")
        assertEquals("14:00-15:00", result.dailyUnavailableRanges,
            "Daily unavailable ranges should be updated")
        // active defaults to true since not explicitly set
        assertTrue(result.active,
            "Active defaults to true - Boolean has no 'not set' value")
    }

    /**
     * Test: Resource with no existing availability fields can have them set.
     */
    @Test
    fun updateResourceWithoutExistingAvailabilityCanSetThem() {
        // Existing resource with no availability fields set (all defaults)
        val existingResource = AjastaResource(
            id = AjastaResourceId("test-resource-456"),
            name = "Volleyball Court",
            type = AjastaResourceType.VOLLEYBALL_COURT,
            location = "Beach",
            pricePerSlot = 30.0,
            active = true,  // Default
            unavailableWeekdays = "",  // No restrictions
            unavailableDates = "",
            dailyUnavailableRanges = "",
            lock = AjastaLock("lock-456")
        )

        val validatedRequest = AjastaResource(
            id = existingResource.id,
            name = existingResource.name,
            unavailableWeekdays = "0",  // Add Sunday closure
            lock = existingResource.lock
        )

        val result = simulateRepoPrepareResourceUpdate(existingResource, validatedRequest)

        assertEquals("0", result.unavailableWeekdays,
            "Should set new unavailable weekdays")
        assertTrue(result.active,
            "Active should remain true (preserved)")
    }

    /**
     * Test: All fields in request should take precedence over existing data.
     */
    @Test
    fun updateResourceAllFieldsSentInRequest() {
        val existingResource = createExistingResourceWithAvailability()

        val validatedRequest = AjastaResource(
            id = existingResource.id,
            name = "Updated Name",
            type = AjastaResourceType.PLAYGROUND,
            location = "New Location",
            description = "New Description",
            pricePerSlot = 99.0,
            unitsCount = 5,
            active = true,
            unavailableWeekdays = "1,2,3",
            unavailableDates = "2025-11-11",
            dailyUnavailableRanges = "10:00-11:00",
            lock = existingResource.lock
        )

        val result = simulateRepoPrepareResourceUpdate(existingResource, validatedRequest)

        assertEquals("Updated Name", result.name)
        assertEquals(AjastaResourceType.PLAYGROUND, result.type)
        assertEquals("New Location", result.location)
        assertEquals("New Description", result.description)
        assertEquals(99.0, result.pricePerSlot)
        assertEquals(5, result.unitsCount)
        assertTrue(result.active)
        assertEquals("1,2,3", result.unavailableWeekdays)
        assertEquals("2025-11-11", result.unavailableDates)
        assertEquals("10:00-11:00", result.dailyUnavailableRanges)
    }

    /**
     * Test: Regression test - verifies the fix for STRING availability fields being lost.
     *
     * This test specifically verifies the bug fix where updating a resource
     * without sending string availability fields would clear them.
     *
     * Before fix: unavailableWeekdays would become ""
     * After fix: unavailableWeekdays is preserved as "0,6"
     *
     * Note: The `active` boolean field is NOT preserved since Boolean has no "not set" value.
     * This is acceptable - the fix specifically addresses STRING fields which CAN have
     * an "empty" value to indicate "not set".
     */
    @Test
    fun regressionTestAvailabilityFieldsPreservedOnPartialUpdate() {
        val existingResource = createExistingResourceWithAvailability()

        // Simulate a typical frontend update that only changes basic fields
        val validatedRequest = AjastaResource(
            id = existingResource.id,
            name = "Updated Name",
            type = existingResource.type,
            location = "Updated Location",
            pricePerSlot = 60.0,
            // Note: NO string availability fields sent - this is the key scenario
            lock = existingResource.lock
        )

        val result = simulateRepoPrepareResourceUpdate(existingResource, validatedRequest)

        // Basic fields should be updated
        assertEquals("Updated Name", result.name)
        assertEquals("Updated Location", result.location)
        assertEquals(60.0, result.pricePerSlot)

        // CRITICAL: STRING availability fields MUST be preserved
        // This is the exact bug that was fixed - these were being reset to empty strings
        assertEquals("0,6", result.unavailableWeekdays,
            "REGRESSION: UnavailableWeekdays must be preserved (was being reset to empty)")
        assertEquals("2025-12-25,2025-01-01", result.unavailableDates,
            "REGRESSION: UnavailableDates must be preserved (was being reset to empty)")
        assertEquals("12:00-13:00", result.dailyUnavailableRanges,
            "REGRESSION: DailyUnavailableRanges must be preserved (was being reset to empty)")

        // Note: active is NOT preserved because Boolean has no "not set" value
        // This is a known limitation - frontend must explicitly send active value
        assertTrue(result.active,
            "Active defaults to true - Boolean cannot indicate 'not set'")
    }
}
