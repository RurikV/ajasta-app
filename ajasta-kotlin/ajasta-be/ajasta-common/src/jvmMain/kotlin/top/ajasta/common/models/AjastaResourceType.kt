package top.ajasta.common.models

/**
 * Types of bookable resources in the system.
 */
enum class AjastaResourceType {
    NONE,
    TURF_COURT,
    VOLLEYBALL_COURT,
    PLAYGROUND,
    HAIRDRESSING_CHAIR,
    OTHER;

    companion object {
        fun fromString(value: String?) = when (value?.lowercase()) {
            "turfcourt", "turf_court" -> TURF_COURT
            "volleyballcourt", "volleyball_court" -> VOLLEYBALL_COURT
            "playground" -> PLAYGROUND
            "hairdressingchair", "hairdressing_chair" -> HAIRDRESSING_CHAIR
            "other" -> OTHER
            else -> NONE
        }
    }
}
