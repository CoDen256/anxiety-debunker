package io.github.coden.anxiety.debunker.core.persistance

import java.time.Instant

data class Anxiety(
    val description: String,
    val id: String,
    val created: Instant = Instant.now(),
    val resolution: Resolution? = null,
    val chanceAssessments: List<ChanceAssessment> = emptyList()
)
