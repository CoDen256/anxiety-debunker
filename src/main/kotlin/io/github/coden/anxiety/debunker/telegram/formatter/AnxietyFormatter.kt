package io.github.coden.anxiety.debunker.telegram.formatter

import io.github.coden.anxiety.debunker.core.api.AnxietyResolutionResponse
import io.github.coden.anxiety.debunker.core.api.AnxietyListResponse
import io.github.coden.telegram.senders.StyledString
import java.time.Instant

interface AnxietyFormatter {
    fun tableWithResolutions(response: AnxietyListResponse): StyledString
    fun tableShort(response: AnxietyListResponse): StyledString
    fun resolution(resolution: AnxietyResolutionResponse): StyledString
    fun anxiety(id: String, created: Instant, description: String, resolution: AnxietyResolutionResponse): StyledString
    fun deletedAnxiety(id: String): StyledString
    fun callbackAnswer(id: String): StyledString
}