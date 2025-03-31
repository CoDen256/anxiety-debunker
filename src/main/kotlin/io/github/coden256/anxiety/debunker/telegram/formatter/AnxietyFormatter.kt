package io.github.coden256.anxiety.debunker.telegram.formatter

import io.github.coden256.anxiety.debunker.core.api.AnxietyListResponse
import io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionType
import io.github.coden256.telegram.senders.StyledString
import java.time.Instant

interface AnxietyFormatter {
    fun table(response: AnxietyListResponse): StyledString
    fun tableConcise(response: AnxietyListResponse): StyledString
    fun listVerbose(response: AnxietyListResponse): StyledString
    fun list(response: AnxietyListResponse): StyledString
    fun listConcise(response: AnxietyListResponse): StyledString
    fun listVeryConcise(response: AnxietyListResponse): StyledString
    fun resolution(resolution: AnxietyResolutionType): StyledString
    fun anxiety(anxiety: AnxietyEntity): StyledString
    fun deletedAnxiety(id: String): StyledString
    fun callbackAnswer(id: String): String
}

data class AnxietyEntity(
    val id: String,
    val created: Instant,
    val description: String,
    val resolution: AnxietyResolutionType,
    val trigger: String? = null,
    val bodyResponse: String? = null,
    val anxietyResponse: String? = null,
    val alternativeThoughts: String? = null,
)