package top.ajasta.common.models

/**
 * State of the request processing.
 */
enum class AjastaState {
    NONE,
    RUNNING,
    FAILING,
    FINISHING
}
