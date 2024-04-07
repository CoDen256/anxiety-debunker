package coden.anxiety.debunker.telebot

import coden.anxiety.debunker.core.api.AnxietyEntityResolution
import coden.anxiety.debunker.core.api.AnxietyListResponse
import java.time.Instant

interface AnxietyFormatter {
    fun format(response: AnxietyListResponse): String
    fun formatResolution(resolution: AnxietyEntityResolution): String
    fun formatAnxiety(id: String, created: Instant, description: String, resolution: AnxietyEntityResolution): String
}