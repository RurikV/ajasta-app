package top.ajasta.repo.inmemory

import top.ajasta.repo.RepoBookingInitialized
import top.ajasta.repo.tests.booking.*

class RepoBookingInMemoryCreateTest : RepoBookingCreateTest() {
    override val repo = RepoBookingInitialized(
        RepoBookingInMemory(randomUuid = { uuidNew.asString() }),
        initObjects = initObjects.toList()
    )
}

class RepoBookingInMemoryReadTest : RepoBookingReadTest() {
    override val repo = RepoBookingInitialized(
        RepoBookingInMemory(),
        initObjects = initObjects.toList()
    )
}

class RepoBookingInMemoryUpdateTest : RepoBookingUpdateTest() {
    override val repo = RepoBookingInitialized(
        RepoBookingInMemory(randomUuid = { lockNew.asString() }),
        initObjects = initObjects.toList()
    )
}

class RepoBookingInMemoryDeleteTest : RepoBookingDeleteTest() {
    override val repo = RepoBookingInitialized(
        RepoBookingInMemory(),
        initObjects = initObjects.toList()
    )
}

class RepoBookingInMemorySearchTest : RepoBookingSearchTest() {
    override val repo = RepoBookingInitialized(
        RepoBookingInMemory(),
        initObjects = initObjects.toList()
    )
}
