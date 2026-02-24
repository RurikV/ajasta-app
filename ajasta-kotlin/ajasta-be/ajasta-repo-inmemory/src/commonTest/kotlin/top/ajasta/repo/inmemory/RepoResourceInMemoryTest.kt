package top.ajasta.repo.inmemory

import top.ajasta.repo.RepoResourceInitialized
import top.ajasta.repo.tests.resource.*

class RepoResourceInMemoryCreateTest : RepoResourceCreateTest() {
    override val repo = RepoResourceInitialized(
        RepoResourceInMemory(randomUuid = { uuidNew.asString() }),
        initObjects = initObjects.toList()
    )
}

class RepoResourceInMemoryReadTest : RepoResourceReadTest() {
    override val repo = RepoResourceInitialized(
        RepoResourceInMemory(),
        initObjects = initObjects.toList()
    )
}

class RepoResourceInMemoryUpdateTest : RepoResourceUpdateTest() {
    override val repo = RepoResourceInitialized(
        RepoResourceInMemory(randomUuid = { lockNew.asString() }),
        initObjects = initObjects.toList()
    )
}

class RepoResourceInMemoryDeleteTest : RepoResourceDeleteTest() {
    override val repo = RepoResourceInitialized(
        RepoResourceInMemory(),
        initObjects = initObjects.toList()
    )
}

class RepoResourceInMemorySearchTest : RepoResourceSearchTest() {
    override val repo = RepoResourceInitialized(
        RepoResourceInMemory(),
        initObjects = initObjects.toList()
    )
}

class RepoResourceInMemoryAvailabilityFieldsTest : RepoResourceAvailabilityFieldsTest() {
    override val repo = RepoResourceInitialized(
        RepoResourceInMemory(),
        initObjects = initObjects.toList()
    )
}
