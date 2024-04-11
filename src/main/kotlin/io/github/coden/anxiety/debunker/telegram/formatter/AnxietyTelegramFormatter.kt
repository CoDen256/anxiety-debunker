package io.github.coden.anxiety.debunker.telegram.formatter

import io.github.coden.anxiety.debunker.core.api.AnxietyListResponse
import org.sk.PrettyTable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AnxietyTelegramFormatter: AnxietyFormatter {

    private val formatter = DateTimeFormatter.ofPattern("d MMM HH:mm")
    private val short = DateTimeFormatter.ofPattern("dd.MM HH:mm")

    override fun formatTableWithResolutions(response: AnxietyListResponse): String{
        val table = PrettyTable("created", "id", "anxiety")
        for (anxiety in response.anxieties.sortedBy { it.created }){
            val created = short.format(anxiety.created.atZone(ZoneId.of("CET")))
            val res = formatResolution(anxiety.resolution)
            table.addRow("$res $created","#${anxiety.id}", anxiety.description.take(15))
        }
        return table.toString()
    }

    override fun formatTableShort(response: io.github.coden.anxiety.debunker.core.api.AnxietyListResponse): String {
        val table = PrettyTable( "id", "anxiety")
        for (anxiety in response.anxieties.sortedBy { it.created }){
            table.addRow("#${anxiety.id}", anxiety.description.take(20).padEnd(20,' '))
        }
        return table.toString()
    }

    override fun formatAnxiety(id: String, created: Instant, description: String, resolution: io.github.coden.anxiety.debunker.core.api.AnxietyResolutionResponse): String {
        return "*Anxiety* `#${id}` ${formatResolution(resolution)}" +
                "\n${formatter.format(created.atZone(ZoneId.of("CET")))}" +
                "\n\n$description"
    }

    override fun formatDeletedAnxiety(id: String): String {
        return "*Anxiety* `#${id}` - `❌REMOVED`"
    }

    override fun formatUpdatedAnxiety(id: String): String {
        return "#${id} - ✅ Successfuly updated"
    }

    override fun formatResolution(resolution: io.github.coden.anxiety.debunker.core.api.AnxietyResolutionResponse): String{
        return when(resolution.type){
            io.github.coden.anxiety.debunker.core.api.AnxietyResolutionType.UNRESOLVED -> "\uD83D\uDD18"
            io.github.coden.anxiety.debunker.core.api.AnxietyResolutionType.FULFILLED -> "🔴"
            io.github.coden.anxiety.debunker.core.api.AnxietyResolutionType.UNFULFILLED -> "🟢"
        }
    }
    private fun formatTableResolution(resolution: io.github.coden.anxiety.debunker.core.api.AnxietyResolutionResponse): String{
        return when(resolution.type){
            io.github.coden.anxiety.debunker.core.api.AnxietyResolutionType.UNRESOLVED -> "▫\uFE0F"
            io.github.coden.anxiety.debunker.core.api.AnxietyResolutionType.FULFILLED -> "🔴"
            io.github.coden.anxiety.debunker.core.api.AnxietyResolutionType.UNFULFILLED -> "🟢"
        }
    }

}