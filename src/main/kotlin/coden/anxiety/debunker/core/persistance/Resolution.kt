package coden.anxiety.debunker.core.persistance

import java.time.Instant

data class Resolution(
    val resolvedAt: Instant,
    val anxietyId: String,
    val fulfilled: Boolean
)