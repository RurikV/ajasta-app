package top.ajasta.stubs

import kotlinx.datetime.Instant
import top.ajasta.common.models.*

object AjastaResourceStubs {
    val RESOURCE_TENNIS_COURT: AjastaResource
        get() = AjastaResource(
            id = AjastaResourceId("resource-tennis-001"),
            name = "Tennis Court A",
            type = AjastaResourceType.TURF_COURT,
            location = "Sports Complex, Building 5",
            description = "Professional tennis court with artificial turf",
            pricePerSlot = 30.0,
            unitsCount = 2,
            openTime = "08:00",
            closeTime = "22:00",
            rating = 4.5,
            reviewCount = 42,
            ownerId = AjastaUserId("owner-001"),
            lock = AjastaLock("lock-res-001")
        )

    val RESOURCE_VOLLEYBALL_COURT: AjastaResource
        get() = AjastaResource(
            id = AjastaResourceId("resource-volley-001"),
            name = "Volleyball Court B",
            type = AjastaResourceType.VOLLEYBALL_COURT,
            location = "Sports Complex, Building 3",
            description = "Indoor volleyball court",
            pricePerSlot = 25.0,
            unitsCount = 1,
            openTime = "09:00",
            closeTime = "21:00",
            rating = 4.3,
            reviewCount = 28,
            ownerId = AjastaUserId("owner-001"),
            lock = AjastaLock("lock-res-002")
        )

    val RESOURCE_HAIRDRESSING: AjastaResource
        get() = AjastaResource(
            id = AjastaResourceId("resource-hair-001"),
            name = "Hairdressing Chair 1",
            type = AjastaResourceType.HAIRDRESSING_CHAIR,
            location = "Beauty Salon, Main Street 10",
            description = "Professional hairdressing station",
            pricePerSlot = 25.0,
            unitsCount = 1,
            openTime = "10:00",
            closeTime = "19:00",
            rating = 4.8,
            reviewCount = 156,
            ownerId = AjastaUserId("owner-002"),
            lock = AjastaLock("lock-res-003")
        )

    val RESOURCE_PLAYGROUND: AjastaResource
        get() = AjastaResource(
            id = AjastaResourceId("resource-play-001"),
            name = "Kids Playground",
            type = AjastaResourceType.PLAYGROUND,
            location = "Central Park",
            description = "Outdoor playground for kids parties",
            pricePerSlot = 50.0,
            unitsCount = 1,
            openTime = "10:00",
            closeTime = "18:00",
            rating = 4.6,
            reviewCount = 89,
            ownerId = AjastaUserId("owner-003"),
            lock = AjastaLock("lock-res-004")
        )
}
