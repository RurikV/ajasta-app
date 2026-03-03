package top.ajasta.repo

import top.ajasta.common.models.AjastaBooking
import top.ajasta.common.models.AjastaResource

/**
 * Delegate for all repositories that allows initializing the database with preloaded data
 */
class RepoBookingInitialized(
    val repo: IRepoBookingInitializable,
    initObjects: Collection<AjastaBooking> = emptyList(),
) : IRepoBookingInitializable by repo {
    @Suppress("unused")
    val initializedObjects: List<AjastaBooking> = repo.save(initObjects).toList()
}

/**
 * Delegate for all repositories that allows initializing the database with preloaded data
 */
class RepoResourceInitialized(
    val repo: IRepoResourceInitializable,
    initObjects: Collection<AjastaResource> = emptyList(),
) : IRepoResourceInitializable by repo {
    @Suppress("unused")
    val initializedObjects: List<AjastaResource> = repo.save(initObjects).toList()
}
