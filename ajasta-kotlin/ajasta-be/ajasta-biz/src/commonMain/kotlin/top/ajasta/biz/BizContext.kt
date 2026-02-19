package top.ajasta.biz

import top.ajasta.common.AjastaContext
import top.ajasta.repo.IRepoBooking
import top.ajasta.repo.IRepoResource

/**
 * Extended context with repository support for business logic processing.
 * Inherits all properties from AjastaContext and adds repository references.
 */
class BizContext : AjastaContext {
    constructor() : super()
    constructor(
        command: top.ajasta.common.models.AjastaCommand = top.ajasta.common.models.AjastaCommand.NONE,
        state: top.ajasta.common.models.AjastaState = top.ajasta.common.models.AjastaState.NONE,
        workMode: top.ajasta.common.models.AjastaWorkMode = top.ajasta.common.models.AjastaWorkMode.PROD,
        stubCase: top.ajasta.common.models.AjastaStubs = top.ajasta.common.models.AjastaStubs.NONE,
        timeStart: kotlinx.datetime.Instant = kotlinx.datetime.Instant.DISTANT_PAST
    ) : super(
        command = command,
        state = state,
        workMode = workMode,
        stubCase = stubCase,
        timeStart = timeStart
    )

    // Repositories
    var repoBooking: IRepoBooking = IRepoBooking.NONE
    var repoResource: IRepoResource = IRepoResource.NONE
}
