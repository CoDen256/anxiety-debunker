package coden.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import org.apache.commons.lang3.RandomStringUtils
import org.apache.logging.log4j.kotlin.KotlinLogger
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom

val singleThreadScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

fun randomPronouncable(min: Int, max: Int): String{
    val current = ThreadLocalRandom.current()
    val result = RandomStringUtils.random(    current.nextInt(min, 2*max),
        vowels.repeat(current.nextInt(10)) + consontants.repeat(current.nextInt(3))
    ).lowercase()
    return result
        .replace(Regex("([$vowels]{2})(?=([$vowels]))")){it.groupValues[1]+ randomConsonant() }
        .replace(Regex("([$consontants]{2})(?=([$consontants]))")){it.groupValues[1]+ randomVowel() }
        .take(max)
}

val vowels = "aeiou"
val consontants = "bcdfghjklmnpqrstvwxyz"

fun String.lastVowel(): Boolean{
    return this.lastOrNull()?.let { vowels.contains(it) } == true
}

fun randomVowel(): String{
    return RandomStringUtils.random(1, vowels)
}

fun randomConsonant(): String{
    return RandomStringUtils.random(1, consontants)
}

fun randomNumber(): String{
    return RandomStringUtils.randomNumeric(1)
}

fun <T> Result<T>.logInteraction(logger: KotlinLogger, operation: (T) -> String): Result<T> {
    return this
        .onSuccess { logger.info("${operation(it)} - Success!") }
        .onFailure { logger.error("Failed! - " + it.message, it) }
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