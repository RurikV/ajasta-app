package top.ajasta.api.v1.mappers

import org.junit.jupiter.api.Test
import top.ajasta.api.v1.models.*
import top.ajasta.common.AjastaContext
import top.ajasta.common.models.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResourceMapperTest {

    // === Create Resource Tests ===

    @Test
    fun `should map ResourceCreateRequest to context`() {
        val request = ResourceCreateRequest(
            requestId = "req-123",
            debug = Debug(
                mode = RequestDebugMode.STUB,
                stub = RequestDebugStubs.SUCCESS
            ),
            resource = ResourceCreateObject(
                name = "Tennis Court A",
                type = ResourceType.TURF_COURT,
                location = "Sports Complex, Building 5",
                description = "Professional tennis court",
                pricePerSlot = 30.0,
                unitsCount = 2,
                openTime = "08:00",
                closeTime = "22:00"
            )
        )

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaCommand.CREATE_RESOURCE, context.command)
        assertEquals("req-123", context.requestId.asString())
        assertEquals(AjastaWorkMode.STUB, context.workMode)
        assertEquals(AjastaStubs.SUCCESS, context.stubCase)
        assertEquals("Tennis Court A", context.resourceRequest.name)
        assertEquals(AjastaResourceType.TURF_COURT, context.resourceRequest.type)
        assertEquals("Sports Complex, Building 5", context.resourceRequest.location)
        assertEquals(30.0, context.resourceRequest.pricePerSlot)
        assertEquals(2, context.resourceRequest.unitsCount)
        assertEquals("08:00", context.resourceRequest.openTime)
        assertEquals("22:00", context.resourceRequest.closeTime)
    }

    @Test
    fun `should map context to ResourceCreateResponse`() {
        val context = AjastaContext(
            command = AjastaCommand.CREATE_RESOURCE,
            requestId = AjastaRequestId("req-123"),
            resourceResponse = AjastaResource(
                id = AjastaResourceId("resource-456"),
                name = "Tennis Court A",
                type = AjastaResourceType.TURF_COURT,
                location = "Sports Complex, Building 5",
                pricePerSlot = 30.0,
                unitsCount = 2,
                rating = 4.5,
                reviewCount = 10,
                ownerId = AjastaUserId("owner-789")
            )
        )

        val response = context.toTransport() as ResourceCreateResponse

        assertEquals("createResource", response.responseType)
        assertEquals("req-123", response.requestId)
        assertNotNull(response.resource)
        assertEquals("resource-456", response.resource?.id)
        assertEquals("Tennis Court A", response.resource?.name)
        assertEquals(ResourceType.TURF_COURT, response.resource?.type)
        assertEquals(30.0, response.resource?.pricePerSlot)
        assertEquals(2, response.resource?.unitsCount)
        assertEquals(4.5, response.resource?.rating)
        assertEquals(10, response.resource?.reviewCount)
        assertEquals("owner-789", response.resource?.ownerId)
    }

    // === Read Resource Tests ===

    @Test
    fun `should map ResourceReadRequest to context`() {
        val request = ResourceReadRequest(
            requestId = "req-123",
            resource = ResourceReadObject(
                id = "resource-456"
            ),
            debug = Debug(mode = RequestDebugMode.PROD)
        )

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaCommand.READ_RESOURCE, context.command)
        assertEquals("resource-456", context.resourceRequest.id.asString())
        assertEquals(AjastaWorkMode.PROD, context.workMode)
    }

    @Test
    fun `should map context to ResourceReadResponse`() {
        val context = AjastaContext(
            command = AjastaCommand.READ_RESOURCE,
            requestId = AjastaRequestId("req-123"),
            resourceResponse = AjastaResource(
                id = AjastaResourceId("resource-456"),
                name = "Volleyball Court B",
                type = AjastaResourceType.VOLLEYBALL_COURT,
                pricePerSlot = 25.0,
                rating = 4.8
            )
        )

        val response = context.toTransport() as ResourceReadResponse

        assertEquals("readResource", response.responseType)
        assertEquals("Volleyball Court B", response.resource?.name)
        assertEquals(ResourceType.VOLLEYBALL_COURT, response.resource?.type)
        assertEquals(4.8, response.resource?.rating)
    }

    // === Update Resource Tests ===

    @Test
    fun `should map ResourceUpdateRequest to context`() {
        val request = ResourceUpdateRequest(
            requestId = "req-123",
            resource = ResourceUpdateObject(
                id = "resource-789",
                name = "Updated Name",
                pricePerSlot = 35.0,
                lock = "lock-v1"
            )
        )

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaCommand.UPDATE_RESOURCE, context.command)
        assertEquals("resource-789", context.resourceRequest.id.asString())
        assertEquals("Updated Name", context.resourceRequest.name)
        assertEquals(35.0, context.resourceRequest.pricePerSlot)
        assertEquals("lock-v1", context.resourceRequest.lock.asString())
    }

    @Test
    fun `should map context to ResourceUpdateResponse`() {
        val context = AjastaContext(
            command = AjastaCommand.UPDATE_RESOURCE,
            requestId = AjastaRequestId("req-123"),
            resourceResponse = AjastaResource(
                id = AjastaResourceId("resource-789"),
                name = "Updated Name",
                pricePerSlot = 35.0,
                lock = AjastaLock("lock-v2")
            )
        )

        val response = context.toTransport() as ResourceUpdateResponse

        assertEquals("updateResource", response.responseType)
        assertEquals("Updated Name", response.resource?.name)
        assertEquals(35.0, response.resource?.pricePerSlot)
        assertEquals("lock-v2", response.resource?.lock)
    }

    // === Delete Resource Tests ===

    @Test
    fun `should map ResourceDeleteRequest to context`() {
        val request = ResourceDeleteRequest(
            requestId = "req-123",
            resource = ResourceDeleteObject(
                id = "resource-999",
                lock = "lock-v1"
            )
        )

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaCommand.DELETE_RESOURCE, context.command)
        assertEquals("resource-999", context.resourceRequest.id.asString())
        assertEquals("lock-v1", context.resourceRequest.lock.asString())
    }

    @Test
    fun `should map context to ResourceDeleteResponse`() {
        val context = AjastaContext(
            command = AjastaCommand.DELETE_RESOURCE,
            requestId = AjastaRequestId("req-123"),
            resourceResponse = AjastaResource(
                id = AjastaResourceId("resource-999")
            )
        )

        val response = context.toTransport() as ResourceDeleteResponse

        assertEquals("deleteResource", response.responseType)
        assertEquals("resource-999", response.resource?.id)
    }

    // === Search Resources Tests ===

    @Test
    fun `should map ResourceSearchRequest to context`() {
        val request = ResourceSearchRequest(
            requestId = "req-123",
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

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaCommand.SEARCH_RESOURCES, context.command)
        assertEquals(AjastaResourceType.TURF_COURT, context.resourceFilterRequest.type)
        assertEquals("Sports Complex", context.resourceFilterRequest.location)
        assertEquals(20.0, context.resourceFilterRequest.minPrice)
        assertEquals(50.0, context.resourceFilterRequest.maxPrice)
        assertEquals(4.0, context.resourceFilterRequest.minRating)
        assertEquals(1, context.page)
        assertEquals(10, context.pageSize)
    }

    @Test
    fun `should map context to ResourceSearchResponse`() {
        val context = AjastaContext(
            command = AjastaCommand.SEARCH_RESOURCES,
            requestId = AjastaRequestId("req-123"),
            resourcesResponse = mutableListOf(
                AjastaResource(
                    id = AjastaResourceId("resource-1"),
                    name = "Tennis Court A",
                    type = AjastaResourceType.TURF_COURT,
                    pricePerSlot = 30.0,
                    rating = 4.5
                ),
                AjastaResource(
                    id = AjastaResourceId("resource-2"),
                    name = "Tennis Court B",
                    type = AjastaResourceType.TURF_COURT,
                    pricePerSlot = 35.0,
                    rating = 4.7
                )
            )
        )

        val response = context.toTransport() as ResourceSearchResponse

        assertEquals("searchResources", response.responseType)
        assertEquals(2, response.resources?.size)
        assertEquals("Tennis Court A", response.resources?.get(0)?.name)
        assertEquals("Tennis Court B", response.resources?.get(1)?.name)
    }

    // === Resource Type Mapping Tests ===

    @Test
    fun `should map all resource types correctly`() {
        val types = listOf(
            ResourceType.TURF_COURT to AjastaResourceType.TURF_COURT,
            ResourceType.VOLLEYBALL_COURT to AjastaResourceType.VOLLEYBALL_COURT,
            ResourceType.PLAYGROUND to AjastaResourceType.PLAYGROUND,
            ResourceType.HAIRDRESSING_CHAIR to AjastaResourceType.HAIRDRESSING_CHAIR,
            ResourceType.OTHER to AjastaResourceType.OTHER
        )

        types.forEach { (transportType, internalType) ->
            val request = ResourceCreateRequest(
                resource = ResourceCreateObject(
                    name = "Test Resource",
                    type = transportType,
                    pricePerSlot = 10.0
                )
            )

            val context = AjastaContext()
            context.fromTransport(request)

            assertEquals(internalType, context.resourceRequest.type)

            // Test reverse mapping
            context.resourceResponse = AjastaResource(
                id = AjastaResourceId("res-1"),
                type = internalType
            )
            context.command = AjastaCommand.CREATE_RESOURCE

            val response = context.toTransport() as ResourceCreateResponse
            assertEquals(transportType, response.resource?.type)
        }
    }

    // === New Fields Tests (active, unavailableWeekdays, unavailableDates, dailyUnavailableRanges) ===

    @Test
    fun `should map new availability fields from ResourceCreateRequest to context`() {
        val request = ResourceCreateRequest(
            requestId = "req-new-fields",
            resource = ResourceCreateObject(
                name = "Test Resource",
                type = ResourceType.TURF_COURT,
                pricePerSlot = 50.0,
                active = false,
                unavailableWeekdays = "0,6",
                unavailableDates = "2025-01-01,2025-12-25",
                dailyUnavailableRanges = "12:00-13:00;17:00-18:00"
            )
        )

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaCommand.CREATE_RESOURCE, context.command)
        assertEquals(false, context.resourceRequest.active)
        assertEquals("0,6", context.resourceRequest.unavailableWeekdays)
        assertEquals("2025-01-01,2025-12-25", context.resourceRequest.unavailableDates)
        assertEquals("12:00-13:00;17:00-18:00", context.resourceRequest.dailyUnavailableRanges)
    }

    @Test
    fun `should map new availability fields from ResourceUpdateRequest to context`() {
        val request = ResourceUpdateRequest(
            requestId = "req-update-fields",
            resource = ResourceUpdateObject(
                id = "resource-update-123",
                name = "Updated Resource",
                pricePerSlot = 75.0,
                active = true,
                unavailableWeekdays = "1",
                unavailableDates = "2025-07-04",
                dailyUnavailableRanges = "14:00-15:00",
                lock = "lock-123"
            )
        )

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaCommand.UPDATE_RESOURCE, context.command)
        assertEquals("resource-update-123", context.resourceRequest.id.asString())
        assertEquals(true, context.resourceRequest.active)
        assertEquals("1", context.resourceRequest.unavailableWeekdays)
        assertEquals("2025-07-04", context.resourceRequest.unavailableDates)
        assertEquals("14:00-15:00", context.resourceRequest.dailyUnavailableRanges)
        assertEquals("lock-123", context.resourceRequest.lock.asString())
    }

    @Test
    fun `should map new availability fields from context to ResourceCreateResponse`() {
        val context = AjastaContext(
            command = AjastaCommand.CREATE_RESOURCE,
            requestId = AjastaRequestId("req-response"),
            resourceResponse = AjastaResource(
                id = AjastaResourceId("resource-new-fields"),
                name = "Test Resource",
                type = AjastaResourceType.TURF_COURT,
                pricePerSlot = 50.0,
                active = false,
                unavailableWeekdays = "0,6",
                unavailableDates = "2025-01-01,2025-12-25",
                dailyUnavailableRanges = "12:00-13:00;17:00-18:00"
            )
        )

        val response = context.toTransport() as ResourceCreateResponse

        assertEquals("createResource", response.responseType)
        assertEquals(false, response.resource?.active)
        assertEquals("0,6", response.resource?.unavailableWeekdays)
        assertEquals("2025-01-01,2025-12-25", response.resource?.unavailableDates)
        assertEquals("12:00-13:00;17:00-18:00", response.resource?.dailyUnavailableRanges)
    }

    @Test
    fun `should map new availability fields from context to ResourceUpdateResponse`() {
        val context = AjastaContext(
            command = AjastaCommand.UPDATE_RESOURCE,
            requestId = AjastaRequestId("req-update-response"),
            resourceResponse = AjastaResource(
                id = AjastaResourceId("resource-updated"),
                name = "Updated Resource",
                active = true,
                unavailableWeekdays = "1",
                unavailableDates = "2025-07-04",
                dailyUnavailableRanges = "14:00-15:00",
                lock = AjastaLock("new-lock")
            )
        )

        val response = context.toTransport() as ResourceUpdateResponse

        assertEquals("updateResource", response.responseType)
        assertEquals(true, response.resource?.active)
        assertEquals("1", response.resource?.unavailableWeekdays)
        assertEquals("2025-07-04", response.resource?.unavailableDates)
        assertEquals("14:00-15:00", response.resource?.dailyUnavailableRanges)
    }

    @Test
    fun `should map empty availability fields as null in response`() {
        val context = AjastaContext(
            command = AjastaCommand.CREATE_RESOURCE,
            requestId = AjastaRequestId("req-empty"),
            resourceResponse = AjastaResource(
                id = AjastaResourceId("resource-empty"),
                name = "Always Available Resource",
                type = AjastaResourceType.VOLLEYBALL_COURT,
                pricePerSlot = 25.0,
                active = true,
                unavailableWeekdays = "",
                unavailableDates = "",
                dailyUnavailableRanges = ""
            )
        )

        val response = context.toTransport() as ResourceCreateResponse

        assertEquals(true, response.resource?.active)
        assertNull(response.resource?.unavailableWeekdays)
        assertNull(response.resource?.unavailableDates)
        assertNull(response.resource?.dailyUnavailableRanges)
    }

    @Test
    fun `should use default values for new fields when not provided in create request`() {
        val request = ResourceCreateRequest(
            requestId = "req-defaults",
            resource = ResourceCreateObject(
                name = "Minimal Resource",
                type = ResourceType.PLAYGROUND,
                pricePerSlot = 15.0
                // active, unavailableWeekdays, unavailableDates, dailyUnavailableRanges not provided
            )
        )

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(true, context.resourceRequest.active) // Default is true
        assertEquals("", context.resourceRequest.unavailableWeekdays) // Default is empty
        assertEquals("", context.resourceRequest.unavailableDates) // Default is empty
        assertEquals("", context.resourceRequest.dailyUnavailableRanges) // Default is empty
    }

    @Test
    fun `should map all fields correctly in search response`() {
        val context = AjastaContext(
            command = AjastaCommand.SEARCH_RESOURCES,
            requestId = AjastaRequestId("req-search"),
            resourcesResponse = mutableListOf(
                AjastaResource(
                    id = AjastaResourceId("resource-1"),
                    name = "Resource with all fields",
                    type = AjastaResourceType.HAIRDRESSING_CHAIR,
                    pricePerSlot = 40.0,
                    active = false,
                    unavailableWeekdays = "0",
                    unavailableDates = "2025-01-01",
                    dailyUnavailableRanges = "12:00-13:00"
                ),
                AjastaResource(
                    id = AjastaResourceId("resource-2"),
                    name = "Resource with defaults",
                    type = AjastaResourceType.OTHER,
                    pricePerSlot = 20.0,
                    active = true,
                    unavailableWeekdays = "",
                    unavailableDates = "",
                    dailyUnavailableRanges = ""
                )
            )
        )

        val response = context.toTransport() as ResourceSearchResponse

        assertEquals("searchResources", response.responseType)
        assertEquals(2, response.resources?.size)

        // First resource - with values
        assertEquals(false, response.resources?.get(0)?.active)
        assertEquals("0", response.resources?.get(0)?.unavailableWeekdays)
        assertEquals("2025-01-01", response.resources?.get(0)?.unavailableDates)
        assertEquals("12:00-13:00", response.resources?.get(0)?.dailyUnavailableRanges)

        // Second resource - defaults (empty becomes null)
        assertEquals(true, response.resources?.get(1)?.active)
        assertNull(response.resources?.get(1)?.unavailableWeekdays)
        assertNull(response.resources?.get(1)?.unavailableDates)
        assertNull(response.resources?.get(1)?.dailyUnavailableRanges)
    }

    // === Availability Tests ===

    @Test
    fun `should map AvailabilityRequest to context`() {
        val request = AvailabilityRequest(
            requestId = "req-123",
            resourceId = "resource-456",
            dateFrom = "2025-03-01T00:00:00Z",
            dateTo = "2025-03-01T23:59:59Z"
        )

        val context = AjastaContext()
        context.fromTransport(request)

        assertEquals(AjastaCommand.GET_AVAILABILITY, context.command)
        assertEquals("resource-456", context.availabilityResourceId.asString())
        assertNotNull(context.availabilityDateFrom)
        assertNotNull(context.availabilityDateTo)
    }

    @Test
    fun `should map context to AvailabilityResponse`() {
        val context = AjastaContext(
            command = AjastaCommand.GET_AVAILABILITY,
            requestId = AjastaRequestId("req-123"),
            availabilityResourceId = AjastaResourceId("resource-456"),
            availableSlots = mutableListOf(
                AjastaSlot(
                    slotStart = kotlinx.datetime.Instant.parse("2025-03-01T10:00:00Z"),
                    slotEnd = kotlinx.datetime.Instant.parse("2025-03-01T11:00:00Z"),
                    price = 30.0
                )
            )
        )

        val response = context.toTransport() as AvailabilityResponse

        assertEquals("getAvailability", response.responseType)
        assertEquals("resource-456", response.resourceId)
        assertEquals(1, response.slots?.size)
        assertEquals(30.0, response.slots?.first()?.price)
    }
}
