package top.ajasta.repo.tests.resource

/**
 * Interface for test data initialization.
 * Implementations provide test objects for repository tests.
 */
interface IInitObjects<T> {
    val initObjects: Collection<T>
}
