package coden.anxiety.debunker.core.persistance

import java.time.Instant

data class Anxiety(
    val id: String,
    val description: String,
    val created: Instant
)
