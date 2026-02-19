package top.ajasta.repo.pg

/**
 * SQL field names for the database schema.
 */
object SqlFields {
    // Common fields
    const val ID = "id"
    const val LOCK = "lock"

    // Booking fields
    const val BOOKING_TITLE = "title"
    const val BOOKING_DESCRIPTION = "description"
    const val BOOKING_RESOURCE_ID = "resource_id"
    const val BOOKING_USER_ID = "user_id"
    const val BOOKING_STATUS = "status"
    const val BOOKING_SLOTS = "slots"
    const val BOOKING_CREATED_AT = "created_at"
    const val BOOKING_UPDATED_AT = "updated_at"

    // Resource fields
    const val RESOURCE_NAME = "name"
    const val RESOURCE_DESCRIPTION = "description"
    const val RESOURCE_TYPE = "type"
    const val RESOURCE_LOCATION = "location"
    const val RESOURCE_PRICE = "price_per_slot"
    const val RESOURCE_RATING = "rating"
    const val RESOURCE_OWNER_ID = "owner_id"
    const val RESOURCE_CREATED_AT = "created_at"
    const val RESOURCE_UPDATED_AT = "updated_at"
}
