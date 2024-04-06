package coden.anxiety.debunker.core.api.persistance

import java.time.Instant

data class Resolution(
    val anxietyId: String,
    val fulfilled: Boolean,
    val resolvedAt: Instant = Instant.now()
)