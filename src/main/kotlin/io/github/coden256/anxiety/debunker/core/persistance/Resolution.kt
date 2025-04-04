package io.github.coden256.anxiety.debunker.core.persistance

import java.time.Instant

data class Resolution(
    val anxietyId: String,
    val fulfilled: Boolean,
    val created: Instant = Instant.now()
)