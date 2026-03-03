package top.ajasta.common.helpers

import top.ajasta.common.AjastaContext
import top.ajasta.common.models.AjastaError
import top.ajasta.common.models.AjastaState

fun Throwable.asAjastaError(
    code: String = "unknown",
    group: String = "exceptions",
    message: String = this.message ?: "",
) = AjastaError(
    code = code,
    group = group,
    field = "",
    message = message,
    exception = this,
)

fun AjastaContext.addError(error: AjastaError) = errors.add(error)
fun AjastaContext.addErrors(error: Collection<AjastaError>) = errors.addAll(error)

fun AjastaContext.fail(error: AjastaError) {
    addError(error)
    state = AjastaState.FAILING
}

fun AjastaContext.fail(errors: Collection<AjastaError>) {
    addErrors(errors)
    state = AjastaState.FAILING
}

fun errorValidation(
    field: String,
    violationCode: String,
    description: String,
) = AjastaError(
    code = "validation-$field-$violationCode",
    field = field,
    group = "validation",
    message = "Validation error for field $field: $description",
)

fun errorSystem(
    violationCode: String,
    e: Throwable,
) = AjastaError(
    code = "system-$violationCode",
    group = "system",
    message = "System error occurred. Our staff has been informed, please retry later",
    exception = e,
)
