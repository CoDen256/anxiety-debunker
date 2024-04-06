package coden.anxiety.debunker.core.api.persistance

import org.apache.commons.lang3.RandomStringUtils
import java.time.Instant

data class ResolvedAnxiety(
    val description: String,
    val resolution: AnxietyResolution,
    val id: String = RandomStringUtils.randomNumeric(8),
    val created: Instant = Instant.now()
)

enum class AnxietyResolution{ FULFILLED, UNFULFILLED, UNRESOLVED }