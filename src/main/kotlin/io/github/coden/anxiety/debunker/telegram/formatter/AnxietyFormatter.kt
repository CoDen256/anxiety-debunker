package io.github.coden.anxiety.debunker.telegram.formatter

import io.github.coden.anxiety.debunker.core.api.AnxietyResolutionResponse
import io.github.coden.anxiety.debunker.core.api.AnxietyListResponse
import io.github.coden.telegram.senders.StyledString
import java.time.Instant

interface AnxietyFormatter {
    fun table(response: AnxietyListResponse): StyledString
    fun tableConcise(response: AnxietyListResponse): StyledString
    fun listVerbose(response: AnxietyListResponse): StyledString
    fun list(response: AnxietyListResponse): StyledString
    fun listConcise(response: AnxietyListResponse): StyledString
    fun listVeryConcise(response: AnxietyListResponse): StyledString
    fun resolution(resolution: AnxietyResolutionResponse): StyledString
    fun anxiety(id: String, created: Instant, description: String, resolution: AnxietyResolutionResponse): StyledString
    fun deletedAnxiety(id: String): StyledString
    fun callbackAnswer(id: String): String
}