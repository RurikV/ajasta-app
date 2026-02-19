package top.ajasta.biz

import kotlinx.coroutines.test.runTest
import top.ajasta.biz.BizContext
import top.ajasta.common.models.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResourceValidationTest {

    private val processor = AjastaProcessor()

    @Test
    fun createResourceEmptyName() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            resourceRequest = AjastaResource(
                name = "",
                description = "Test Description",
                pricePerSlot = 50.0
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.field == "name" })
    }

    @Test
    fun createResourceNameTooLong() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            resourceRequest = AjastaResource(
                name = "x".repeat(101),
                description = "Test Description",
                pricePerSlot = 50.0
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.code == "validation-name-length" })
    }

    @Test
    fun createResourceNegativePrice() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            resourceRequest = AjastaResource(
                name = "Test Resource",
                description = "Test Description",
                pricePerSlot = -10.0
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.field == "pricePerSlot" })
    }

    @Test
    fun createResourceZeroPrice() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            resourceRequest = AjastaResource(
                name = "Test Resource",
                description = "Test Description",
                pricePerSlot = 0.0
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.code == "validation-price-negative" })
    }

    @Test
    fun readResourceEmptyId() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.READ_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            resourceRequest = AjastaResource(
                id = AjastaResourceId.NONE
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.field == "id" })
    }

    @Test
    fun updateResourceEmptyId() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.UPDATE_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            resourceRequest = AjastaResource(
                id = AjastaResourceId.NONE,
                name = "Test Resource"
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.field == "id" })
    }

    @Test
    fun updateResourceEmptyName() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.UPDATE_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            resourceRequest = AjastaResource(
                id = AjastaResourceId("resource-123"),
                name = ""
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.field == "name" })
    }

    @Test
    fun deleteResourceEmptyId() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.DELETE_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            resourceRequest = AjastaResource(
                id = AjastaResourceId.NONE
            )
        }
        processor.exec(ctx)
        assertEquals(AjastaState.FAILING, ctx.state)
        assertTrue(ctx.errors.any { it.field == "id" })
    }

    @Test
    fun createResourceValidData() = runTest {
        val ctx = BizContext().apply {
            command = AjastaCommand.CREATE_RESOURCE
            state = AjastaState.NONE
            workMode = AjastaWorkMode.PROD
            resourceRequest = AjastaResource(
                name = "Valid Resource",
                description = "Valid Description",
                pricePerSlot = 100.0
            )
        }
        processor.exec(ctx)
        // Validation should pass (no validation errors)
        assertTrue(ctx.errors.none { it.group == "validation" })
    }
}
