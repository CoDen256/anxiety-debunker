package io.github.coden.anxiety.debunker.core.persistance

data class AnxietyDetail(
    val anxietyId: String,
    val trigger: String,
    val bodyResponse: String,
    val anxietyResponse: String,
    val alternativeThoughts: String
)