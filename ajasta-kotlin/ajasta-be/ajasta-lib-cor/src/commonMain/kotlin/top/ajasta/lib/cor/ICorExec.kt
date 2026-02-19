package top.ajasta.lib.cor

/**
 * Code block that processes a context. Has a name and description.
 */
interface ICorExec<T> {
    val title: String
    val description: String
    suspend fun exec(context: T)
}
