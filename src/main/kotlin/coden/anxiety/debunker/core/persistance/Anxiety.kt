package coden.anxiety.debunker.core.persistance

import org.apache.commons.lang3.RandomStringUtils
import java.time.Instant

data class Anxiety(
    val description: String,
    val id: String = RandomStringUtils.randomAlphabetic(    5),
    val created: Instant = Instant.now()
)
