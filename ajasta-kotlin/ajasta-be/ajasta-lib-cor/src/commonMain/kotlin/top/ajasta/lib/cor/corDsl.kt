package top.ajasta.lib.cor

import top.ajasta.lib.cor.handlers.CorChainDsl
import top.ajasta.lib.cor.handlers.CorWorkerDsl

/**
 * Base builder (DSL)
 */
@CorDslMarker
interface ICorExecDsl<T> {
    var title: String
    var description: String
    fun on(function: suspend T.() -> Boolean)
    fun except(function: suspend T.(e: Throwable) -> Unit)

    fun build(): ICorExec<T>
}

/**
 * Builder (DSL) for chains
 */
@CorDslMarker
interface ICorChainDsl<T> : ICorExecDsl<T> {
    fun add(worker: ICorExecDsl<T>)
}

/**
 * Builder (DSL) for workers
 */
@CorDslMarker
interface ICorWorkerDsl<T> : ICorExecDsl<T> {
    fun handle(function: suspend T.() -> Unit)
}

/**
 * Entry point for DSL chain building.
 * Elements are executed sequentially.
 *
 * Example:
 * ```
 *  rootChain<SomeContext> {
 *      worker {
 *      }
 *      chain {
 *          worker(...) {
 *          }
 *          worker(...) {
 *          }
 *      }
 *  }
 * ```
 */
fun <T> rootChain(function: ICorChainDsl<T>.() -> Unit): ICorChainDsl<T> = CorChainDsl<T>().apply(function)

/**
 * Creates a chain whose elements are executed sequentially.
 */
fun <T> ICorChainDsl<T>.chain(function: ICorChainDsl<T>.() -> Unit) {
    add(CorChainDsl<T>().apply(function))
}

/**
 * Creates a worker
 */
fun <T> ICorChainDsl<T>.worker(function: ICorWorkerDsl<T>.() -> Unit) {
    add(CorWorkerDsl<T>().apply(function))
}

/**
 * Creates a worker with default on and except
 */
fun <T> ICorChainDsl<T>.worker(
    title: String,
    description: String = "",
    blockHandle: T.() -> Unit
) {
    add(CorWorkerDsl<T>().also {
        it.title = title
        it.description = description
        it.handle(blockHandle)
    })
}
