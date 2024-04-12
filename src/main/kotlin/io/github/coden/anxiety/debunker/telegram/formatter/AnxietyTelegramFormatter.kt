package io.github.coden.anxiety.debunker.telegram.formatter

import io.github.coden.anxiety.debunker.core.api.AnxietyListResponse
import io.github.coden.anxiety.debunker.core.api.AnxietyResolutionResponse
import io.github.coden.anxiety.debunker.core.api.AnxietyResolutionType
import io.github.coden.telegram.senders.ParseMode
import io.github.coden.telegram.senders.StyledString
import io.github.coden.telegram.senders.snippet
import io.github.coden.telegram.senders.styled
import org.sk.PrettyTable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val MAX_CHARS_WITHOUT_BUTTON = 73

class AnxietyTelegramFormatter : AnxietyFormatter {

    private val formatter = DateTimeFormatter.ofPattern("d MMM HH:mm")
    private val short = DateTimeFormatter.ofPattern("dd.MM-HH:mm")

    // 🟢 It is appendicitis. It hurts on the right side down. Exactly where it
    // 🔴 Hello this is an anxi ety Hello 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 78
    override fun tableWithResolutions(response: AnxietyListResponse): StyledString {
        val result = StringBuilder()
        for (anxiety in response.anxieties.sortedBy { it.created }) {
            val res = resolution(anxiety.resolution)
            val created = short.format(anxiety.created.atZone(ZoneId.of("CET")))
            result
                .append("`#${anxiety.id}`\n")
                .append(
                    ("${created}\n$res\n${anxiety.description}"
                        .take(MAX_CHARS_WITHOUT_BUTTON-1-3)
                        + "...")
                        .snippet(ParseMode.MARKDOWN)
                )
                .append("\n\n\n")
        }
        result.dropLast(1)
        return result.toString().styled(ParseMode.MARKDOWN)
    }

    private fun asTable(response: AnxietyListResponse): StyledString {
        val table = PrettyTable("created", "id", "anxiety")
        for (anxiety in response.anxieties.sortedBy { it.created }) {
            val created = short.format(anxiety.created.atZone(ZoneId.of("CET")))
            val res = resolution(anxiety.resolution)
            table.addRow("$res $created", "#${anxiety.id}", anxiety.description.take(15))
        }
        return table.toString().styled(ParseMode.HTML).snippet()
    }

    override fun tableShort(response: AnxietyListResponse): StyledString {
        val table = PrettyTable("id", "anxiety")
        for (anxiety in response.anxieties.sortedBy { it.created }) {
            val res = resolution(anxiety.resolution)
            table.addRow("$res #${anxiety.id}", anxiety.description.take(20).padEnd(20, ' '))
        }
        return table.toString().styled(ParseMode.HTML).snippet()
    }

    override fun anxiety(
        id: String,
        created: Instant,
        description: String,
        resolution: AnxietyResolutionResponse
    ): StyledString {
        return ("*Anxiety* `#${id}` ${resolution(resolution)}" +
                "\n${formatter.format(created.atZone(ZoneId.of("CET")))}" +
                "\n\n$description")
            .styled(ParseMode.MARKDOWN)
    }

    override fun deletedAnxiety(id: String): StyledString {
        return "*Anxiety* `#${id}` - `❌REMOVED`".styled(ParseMode.MARKDOWN)
    }

    override fun callbackAnswer(id: String): String {
        return "#${id} - ✅ Successfuly updated"
    }

    override fun resolution(resolution: AnxietyResolutionResponse): StyledString {
        return when (resolution.type) {
            AnxietyResolutionType.UNRESOLVED -> "\uD83D\uDD18"
            AnxietyResolutionType.FULFILLED -> "🔴"
            AnxietyResolutionType.UNFULFILLED -> "🟢"
        }.styled()
    }

    private fun formatTableResolution(resolution: AnxietyResolutionResponse): StyledString {
        return when (resolution.type) {
            AnxietyResolutionType.UNRESOLVED -> "▫\uFE0F"
            AnxietyResolutionType.FULFILLED -> "🔴"
            AnxietyResolutionType.UNFULFILLED -> "🟢"
        }.styled()
    }

}