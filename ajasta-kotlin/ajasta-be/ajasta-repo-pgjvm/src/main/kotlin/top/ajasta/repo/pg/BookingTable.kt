package top.ajasta.repo.pg

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import top.ajasta.common.models.*

/**
 * SQL Table definition for bookings.
 */
class BookingTable(tableName: String) : Table(tableName) {
    val id = text(SqlFields.ID)
    val title = text(SqlFields.BOOKING_TITLE)
    val description = text(SqlFields.BOOKING_DESCRIPTION).nullable()
    val resourceId = text(SqlFields.BOOKING_RESOURCE_ID)
    val userId = text(SqlFields.BOOKING_USER_ID)
    val status = text(SqlFields.BOOKING_STATUS)
    val slots = text(SqlFields.BOOKING_SLOTS) // JSON serialized
    val lock = text(SqlFields.LOCK)
    val createdAt = long(SqlFields.BOOKING_CREATED_AT)
    val updatedAt = long(SqlFields.BOOKING_UPDATED_AT).nullable()

    override val primaryKey = PrimaryKey(id)

    fun from(res: ResultRow): AjastaBooking = AjastaBooking(
        id = AjastaBookingId(res[id]),
        title = res[title],
        description = res[description] ?: "",
        resourceId = AjastaResourceId(res[resourceId]),
        userId = AjastaUserId(res[userId]),
        bookingStatus = AjastaBookingStatus.valueOf(res[status]),
        slots = parseSlots(res[slots]),
        lock = AjastaLock(res[lock]),
        createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(res[createdAt]),
        updatedAt = res[updatedAt]?.let { kotlinx.datetime.Instant.fromEpochMilliseconds(it) }
            ?: kotlinx.datetime.Instant.DISTANT_PAST
    )

    fun UpdateBuilder<*>.to(booking: AjastaBooking, randomUuid: () -> String) {
        this[id] = booking.id.takeIf { it != AjastaBookingId.NONE }?.asString() ?: randomUuid()
        this[title] = booking.title
        this[description] = booking.description.takeIf { it.isNotEmpty() }
        this[resourceId] = booking.resourceId.asString()
        this[userId] = booking.userId.asString()
        this[status] = booking.bookingStatus.name
        this[slots] = serializeSlots(booking.slots)
        this[lock] = booking.lock.takeIf { it != AjastaLock.NONE }?.asString() ?: randomUuid()
        this[createdAt] = booking.createdAt.toEpochMilliseconds()
        this[updatedAt] = booking.updatedAt.toEpochMilliseconds()
    }

    private fun parseSlots(json: String): List<AjastaSlot> {
        if (json.isBlank() || json == "[]") return emptyList()
        // Simple JSON parsing for slots
        return try {
            json.removeSurrounding("[", "]")
                .split("},{")
                .filter { it.isNotBlank() }
                .map { slotStr ->
                    val cleaned = slotStr.removeSurrounding("{", "}")
                    val parts = cleaned.split(",").associate { part ->
                        val (key, value) = part.split("=", limit = 2)
                        key.trim() to value.trim()
                    }
                    AjastaSlot(
                        slotStart = kotlinx.datetime.Instant.parse(parts["slotStart"] ?: ""),
                        slotEnd = kotlinx.datetime.Instant.parse(parts["slotEnd"] ?: ""),
                        price = parts["price"]?.toDoubleOrNull() ?: 0.0
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeSlots(slots: List<AjastaSlot>): String {
        if (slots.isEmpty()) return "[]"
        return slots.joinToString(prefix = "[", postfix = "]") { slot ->
            "{slotStart=${slot.slotStart},slotEnd=${slot.slotEnd},price=${slot.price}}"
        }
    }
}
