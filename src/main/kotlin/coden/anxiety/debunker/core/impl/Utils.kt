package coden.anxiety.debunker.core.impl

import org.apache.logging.log4j.kotlin.KotlinLogger


fun <T> Result<T>.logInteraction(logger: KotlinLogger, operation: String): Result<T> {
    return this
        .onSuccess { logger.info("$operation - Success!") }
        .onFailure { logger.error("$operation - Failed! - " + it.message, it) }
}

fun <T : Any> T.success(): Result<T> = Result.success(this)
fun success(): Result<Unit> = Unit.success()

fun <T : Any> T?.success(default: T): Result<T> {
    return this?.success() ?: default.success()
}

fun <T : Any> T?.successOrElse(exception: Exception): Result<T> {
    return this?.success() ?: Result.failure(exception)
}

inline fun <reified T : Any> T?.successOrElse(): Result<T> {
    return this?.success()
        ?: Result.failure(IllegalArgumentException("Element was not found ${T::class.simpleName}"))
}

inline fun <R, T> Result<T>.flatMap(transform: (value: T) -> Result<R>): Result<R> {
    return this.mapCatching { transform(it).getOrThrow() }
}
inline fun <reified E, R> Result<R>.recover(transform: (exception: E) -> R): Result<R> {
    return this.recoverCatching {
        if (it is E){
            return@recoverCatching transform(it)
        }
        else {
            throw it
        }
    }
}