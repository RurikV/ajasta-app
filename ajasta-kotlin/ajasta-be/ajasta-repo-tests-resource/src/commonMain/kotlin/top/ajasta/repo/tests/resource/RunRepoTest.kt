package top.ajasta.repo.tests.resource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

/**
 * Helper function to run repository tests with proper timeout and dispatcher.
 */
fun runRepoTest(testBody: suspend TestScope.() -> Unit) = runTest(timeout = 2.minutes) {
    withContext(Dispatchers.Default) {
        testBody()
    }
}
