package coden.anxiety.debunker.telegram.formatter

import coden.anxiety.debunker.core.api.AnxietyEntityResolution
import coden.anxiety.debunker.core.api.AnxietyEntityResponse
import coden.anxiety.debunker.core.api.AnxietyListResponse
import org.sk.PrettyTable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AnxietyTelegramFormatter: AnxietyFormatter {

    private val formatter = DateTimeFormatter.ofPattern("d MMM HH:mm")
    private val short = DateTimeFormatter.ofPattern("dd.MM HH:mm")

    override fun format(response: AnxietyListResponse): String{
        val table = PrettyTable("created", "id", "anxiety")
        for (anxiety in response.anxieties.sortedBy { it.created }){
            val created = short.format(anxiety.created.atZone(ZoneId.of("CET")))
            val res = formatResolution(anxiety.resolution)
            table.addRow("$res $created", anxiety.id, anxiety.description.take(15))
        }
        return table.toString()
    }

    override fun formatShort(response: AnxietyListResponse): String {
        val table = PrettyTable( "id", "anxiety")
        for (anxiety in response.anxieties.sortedBy { it.created }){
            table.addRow(anxiety.id, anxiety.description.take(15))
        }
        return table.toString()
    }

    override fun formatAnxiety(id: String, created: Instant, description: String, resolution: AnxietyEntityResolution): String {
        return "*Anxiety* #${id} ${formatResolution(resolution)}" +
                "\n${formatter.format(created.atZone(ZoneId.of("CET")))}" +
                "\n\n$description"
    }

    override fun formatResolution(resolution: AnxietyEntityResolution): String{
        return when(resolution){
            AnxietyEntityResolution.UNRESOLVED -> "\uD83D\uDD18"
            AnxietyEntityResolution.FULFILLED -> "🔴"
            AnxietyEntityResolution.UNFULFILLED -> "🟢"
        }
    }
    private fun formatTableResolution(resolution: AnxietyEntityResolution): String{
        return when(resolution){
            AnxietyEntityResolution.UNRESOLVED -> "▫\uFE0F"
            AnxietyEntityResolution.FULFILLED -> "🔴"
            AnxietyEntityResolution.UNFULFILLED -> "🟢"
        }
    }

}