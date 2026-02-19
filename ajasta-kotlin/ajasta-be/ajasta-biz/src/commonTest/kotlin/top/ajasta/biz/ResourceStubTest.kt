package top.ajasta.biz

import kotlinx.coroutines.test.runTest
import top.ajasta.biz.BizContext
import top.ajasta.common.models.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResourceStubTest {

    private val processor = AjastaProcessor()

    @Test
    fun createResourceSuccess() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.SUCCESS
            resourceRequest = AjastaResource(
                name = "Test Resource",
                description = "Test Description",
                pricePerSlot = 50.0
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FINISHING, ctx.state)
        assertTrue(ctx.resourceResponse.name.isNotEmpty())
    }

    @Test
    fun readResourceSuccess() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.READ_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.SUCCESS
            resourceRequest = AjastaResource(
                id = AjastaResourceId("resource-123")
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FINISHING, ctx.state)
        assertTrue(ctx.resourceResponse.id != AjastaResourceId.NONE)
    }

    @Test
    fun readResourceNotFound() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.READ_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.NOT_FOUND
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.code == "not-found" })
    }

    @Test
    fun updateResourceSuccess() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.UPDATE_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.SUCCESS
            resourceRequest = AjastaResource(
                id = AjastaResourceId("resource-123"),
                name = "Updated Name",
                description = "Updated Description"
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FINISHING, ctx.state)
        assertEquals("Updated Name", ctx.resourceResponse.name)
    }

    @Test
    fun deleteResourceSuccess() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.DELETE_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.SUCCESS
            resourceRequest = AjastaResource(
                id = AjastaResourceId("resource-123")
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FINISHING, ctx.state)
    }

    @Test
    fun searchResourcesSuccess() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.SEARCH_RESOURCES
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.SUCCESS
            resourceFilterRequest = AjastaResourceFilter()
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FINISHING, ctx.state)
        assertTrue(ctx.resourcesResponse.isNotEmpty())
    }

    @Test
    fun searchResourcesByType() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.SEARCH_RESOURCES
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.SUCCESS
            resourceFilterRequest = AjastaResourceFilter(
                type = AjastaResourceType.TURF_COURT
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FINISHING, ctx.state)
        assertTrue(ctx.resourcesResponse.all { it.type == AjastaResourceType.TURF_COURT })
    }

    @Test
    fun getAvailabilitySuccess() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.GET_AVAILABILITY
            state = AjastaState.NONE
            workMode = AjastaWorkMode.STUB
            stubCase = AjastaStubs.SUCCESS
            availabilityResourceId = AjastaResourceId("resource-123")
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FINISHING, ctx.state)
        assertTrue(ctx.availableSlots.isNotEmpty())
    }
}
