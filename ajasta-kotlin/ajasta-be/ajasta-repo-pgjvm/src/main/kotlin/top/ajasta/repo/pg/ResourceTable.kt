package top.ajasta.repo.pg

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import top.ajasta.common.models.*

/**
 * SQL Table definition for resources.
 */
class ResourceTable(tableName: String) : Table(tableName) {
    val id = text(SqlFields.ID)
    val name = text(SqlFields.RESOURCE_NAME)
    val description = text(SqlFields.RESOURCE_DESCRIPTION).nullable()
    val resourceType = text(SqlFields.RESOURCE_TYPE)
    val location = text(SqlFields.RESOURCE_LOCATION).nullable()
    val imageUrl = text(SqlFields.RESOURCE_IMAGE_URL).nullable()
    val pricePerSlot = double(SqlFields.RESOURCE_PRICE)
    val unitsCount = integer(SqlFields.RESOURCE_UNITS_COUNT)
    val openTime = text(SqlFields.RESOURCE_OPEN_TIME).nullable()
    val closeTime = text(SqlFields.RESOURCE_CLOSE_TIME).nullable()
    val rating = double(SqlFields.RESOURCE_RATING)
    val ownerId = text(SqlFields.RESOURCE_OWNER_ID)
    val active = bool(SqlFields.RESOURCE_ACTIVE).default(true)
    val unavailableWeekdays = text(SqlFields.RESOURCE_UNAVAILABLE_WEEKDAYS).nullable()
    val unavailableDates = text(SqlFields.RESOURCE_UNAVAILABLE_DATES).nullable()
    val dailyUnavailableRanges = text(SqlFields.RESOURCE_DAILY_UNAVAILABLE_RANGES).nullable()
    val lock = text(SqlFields.LOCK)
    val createdAt = long(SqlFields.RESOURCE_CREATED_AT)
    val updatedAt = long(SqlFields.RESOURCE_UPDATED_AT).nullable()

    override val primaryKey = PrimaryKey(id)

    fun from(res: ResultRow): AjastaResource = AjastaResource(
        id = AjastaResourceId(res[id]),
        name = res[name],
        description = res[description] ?: "",
        type = AjastaResourceType.valueOf(res[resourceType]),
        location = res[location] ?: "",
        imageUrl = res[imageUrl] ?: "",
        pricePerSlot = res[pricePerSlot],
        unitsCount = res[unitsCount],
        openTime = res[openTime] ?: "",
        closeTime = res[closeTime] ?: "",
        rating = res[rating],
        ownerId = AjastaUserId(res[ownerId]),
        active = res[active],
        unavailableWeekdays = res[unavailableWeekdays] ?: "",
        unavailableDates = res[unavailableDates] ?: "",
        dailyUnavailableRanges = res[dailyUnavailableRanges] ?: "",
        lock = AjastaLock(res[lock]),
        createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(res[createdAt]),
        updatedAt = res[updatedAt]?.let { kotlinx.datetime.Instant.fromEpochMilliseconds(it) }
            ?: kotlinx.datetime.Instant.DISTANT_PAST
    )

    fun UpdateBuilder<*>.to(resource: AjastaResource, randomUuid: () -> String) {
        this[id] = resource.id.takeIf { it != AjastaResourceId.NONE }?.asString() ?: randomUuid()
        this[name] = resource.name
        this[description] = resource.description.takeIf { it.isNotEmpty() }
        this[resourceType] = resource.type.name
        this[location] = resource.location.takeIf { it.isNotEmpty() }
        this[imageUrl] = resource.imageUrl.takeIf { it.isNotEmpty() }
        this[pricePerSlot] = resource.pricePerSlot
        this[unitsCount] = resource.unitsCount
        this[openTime] = resource.openTime.takeIf { it.isNotEmpty() }
        this[closeTime] = resource.closeTime.takeIf { it.isNotEmpty() }
        this[rating] = resource.rating
        this[ownerId] = resource.ownerId.asString()
        this[active] = resource.active
        this[unavailableWeekdays] = resource.unavailableWeekdays.takeIf { it.isNotEmpty() }
        this[unavailableDates] = resource.unavailableDates.takeIf { it.isNotEmpty() }
        this[dailyUnavailableRanges] = resource.dailyUnavailableRanges.takeIf { it.isNotEmpty() }
        this[lock] = resource.lock.takeIf { it != AjastaLock.NONE }?.asString() ?: randomUuid()
        this[createdAt] = resource.createdAt.toEpochMilliseconds()
        this[updatedAt] = resource.updatedAt.toEpochMilliseconds()
    }
}
