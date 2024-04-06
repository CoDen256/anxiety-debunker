package coden.anxiety.debunker.core.persistance

import org.apache.commons.lang3.RandomStringUtils
import java.time.Instant

data class AnxietyWithResolution(
    val description: String,
    val id: String = RandomStringUtils.randomNumeric(8),
    val created: Instant = Instant.now(),
    val resolution: Resolution?
)