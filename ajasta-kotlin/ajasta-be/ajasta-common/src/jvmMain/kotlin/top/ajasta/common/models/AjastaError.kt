package top.ajasta.common.models

/**
 * Error representation in the system.
 */
data class AjastaError(
    var code: String = "",
    var group: String = "",
    var field: String = "",
    var message: String = "",
    var exception: Throwable? = null
) {
    fun deepCopy(): AjastaError = copy(
        exception = exception
    )

    companion object {
        val NONE = AjastaError()
    }
}
