package top.ajasta.common.models

/**
 * Status of a booking in its lifecycle.
 */
enum class AjastaBookingStatus {
    NONE,
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED;

    companion object {
        fun fromString(value: String?) = when (value?.lowercase()) {
            "pending" -> PENDING
            "confirmed" -> CONFIRMED
            "cancelled" -> CANCELLED
            "completed" -> COMPLETED
            else -> NONE
        }
    }
}
