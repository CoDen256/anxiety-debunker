package coden.anxiety.debunker.core.persistance

import java.time.Instant

data class Resolution(
    val anxietyId: String,
    val fulfilled: Boolean,
    val resolvedAt: Instant = Instant.now()
)