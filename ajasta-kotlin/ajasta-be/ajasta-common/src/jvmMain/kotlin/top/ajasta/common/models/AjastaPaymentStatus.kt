package top.ajasta.common.models

/**
 * Payment status for a booking.
 */
enum class AjastaPaymentStatus {
    NONE,
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED;

    companion object {
        fun fromString(value: String?) = when (value?.lowercase()) {
            "pending" -> PENDING
            "completed" -> COMPLETED
            "failed" -> FAILED
            "refunded" -> REFUNDED
            else -> NONE
        }
    }
}
