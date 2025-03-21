package io.github.coden256.anxiety.debunker.telegram.formatter

import io.github.coden256.anxiety.debunker.core.api.AnxietyListResponse
import io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionType
import io.github.coden256.telegram.senders.StyledString
import java.time.Instant

interface AnxietyFormatter {
    fun table(response: io.github.coden256.anxiety.debunker.core.api.AnxietyListResponse): StyledString
    fun tableConcise(response: io.github.coden256.anxiety.debunker.core.api.AnxietyListResponse): StyledString
    fun listVerbose(response: io.github.coden256.anxiety.debunker.core.api.AnxietyListResponse): StyledString
    fun list(response: io.github.coden256.anxiety.debunker.core.api.AnxietyListResponse): StyledString
    fun listConcise(response: io.github.coden256.anxiety.debunker.core.api.AnxietyListResponse): StyledString
    fun listVeryConcise(response: io.github.coden256.anxiety.debunker.core.api.AnxietyListResponse): StyledString
    fun resolution(resolution: io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionType): StyledString
    fun anxiety(anxiety: AnxietyEntity): StyledString
    fun deletedAnxiety(id: String): StyledString
    fun callbackAnswer(id: String): String
}

data class AnxietyEntity(
    val id: String,
    val created: Instant,
    val description: String,
    val resolution: io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionType,
    val trigger: String? = null,
    val bodyResponse: String? = null,
    val anxietyResponse: String? = null,
    val alternativeThoughts: String? = null,
)