package top.ajasta.common.models

/**
 * Commands that can be executed by the booking system.
 */
enum class AjastaCommand {
    NONE,
    CREATE_BOOKING,
    READ_BOOKING,
    UPDATE_BOOKING,
    DELETE_BOOKING,
    SEARCH_BOOKINGS,
    CREATE_RESOURCE,
    READ_RESOURCE,
    UPDATE_RESOURCE,
    DELETE_RESOURCE,
    SEARCH_RESOURCES,
    GET_AVAILABILITY
}
