package coden.anxiety.debunker.telegram.formatter

import coden.anxiety.debunker.core.api.AnxietyResolutionResponse
import coden.anxiety.debunker.core.api.AnxietyListResponse
import java.time.Instant

interface AnxietyFormatter {
    fun formatTableWithResolutions(response: AnxietyListResponse): String
    fun formatTableShort(response: AnxietyListResponse): String
    fun formatResolution(resolution: AnxietyResolutionResponse): String
    fun formatAnxiety(id: String, created: Instant, description: String, resolution: AnxietyResolutionResponse): String
    fun formatDeletedAnxiety(id: String): String
    fun formatUpdatedAnxiety(id: String): String
}