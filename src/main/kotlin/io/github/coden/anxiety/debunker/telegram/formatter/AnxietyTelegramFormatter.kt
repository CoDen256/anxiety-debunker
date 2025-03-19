package io.github.coden.anxiety.debunker.telegram.formatter

import io.github.coden.anxiety.debunker.core.api.AnxietyEntityResponse
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

    private val default = DateTimeFormatter.ofPattern("d MMM HH:mm")

    private fun Instant.str(formatter: DateTimeFormatter): String {
        return formatter.format(this.atZone(ZoneId.of("CET")))
    }

    private fun Instant.str(pattern: String): String {
        return str(DateTimeFormatter.ofPattern(pattern))
    }

    override fun table(response: AnxietyListResponse): StyledString {
        return response.asTable("created", "id", "description"){
            val res = formatTableResolution(it.resolution.type)
            arrayOf("$res ${it.created.str("dd.MM.YY")}","#${it.id}", it.description.take(15))
        }
    }

    override fun tableConcise(response: AnxietyListResponse): StyledString {
        return response.asTable("id", "description"){
            val res = formatTableResolution(it.resolution.type)
            arrayOf("$res #${it.id}", it.description.take(29))
        }
    }

    private fun AnxietyListResponse.asTable(vararg headers: String, formatter: (AnxietyEntityResponse) -> Array<String>): StyledString {
        val table = PrettyTable(*headers)
        for (anxiety in anxieties.sortedBy { it.created }) {
            table.addRow(*formatter.invoke(anxiety))
        }
        return table.toString().styled(ParseMode.HTML).snippet()
    }

    override fun listVerbose(response: AnxietyListResponse): StyledString {
        return response.asList("\n\n\n") { r, id, c, desc ->
            val res = resolution(r.type)
            val created = c.str("d MMMM YYYY HH:mm")
            append("`#${id}`\n")
            append("$res $created".snippet(ParseMode.MARKDOWN))
            append("\n${desc.take(MAX_CHARS_WITHOUT_BUTTON-1).strip()}..".snippet(ParseMode.MARKDOWN))
        }
    }

    override fun list(response: AnxietyListResponse): StyledString {
        return response.asList("\n") { r, id, c, desc ->
            val res = resolution(r.type)
            append(
                ("$res   $id\n${desc}"
                    .take(MAX_CHARS_WITHOUT_BUTTON)
                    .strip() + "..")
                    .snippet(ParseMode.MARKDOWN))
        }

    }

    override fun listConcise(response: AnxietyListResponse): StyledString {
        return response.asList("\n") {r, id, c, desc ->
            val res = resolution(r.type)
            append(
                ("$res ${id} ${desc}"
                    .take(MAX_CHARS_WITHOUT_BUTTON-2)
                    .strip()
                        + "..")
                    .replace(" ", " ")
                    .snippet(ParseMode.MARKDOWN))
        }
    }

    override fun listVeryConcise(response: AnxietyListResponse): StyledString {
        return response.asList("\n") {r, id, c, desc ->
            val res = resolution(r.type)
            append(
                ("$res ${id.padEnd(5)} · ${desc.take(23)}.."
                    .strip()
                    .padEnd(33)

                        + "")
                    .replace(" ", " ")
                    .snippet(ParseMode.MARKDOWN))
        }
    }

    private fun AnxietyListResponse.asList(separator: String, formatter: StringBuilder.(AnxietyResolutionResponse, String, Instant, String) -> Unit): StyledString {
        val result = StringBuilder()
        for (anxiety in anxieties.sortedBy { it.created }) {
            formatter.invoke(result, anxiety.resolution, anxiety.id, anxiety.created, anxiety.description)
            result.append(separator)
        }
        result.dropLast(separator.length)
        return result.toString().styled(ParseMode.MARKDOWN)
    }

    override fun anxiety(
        anxiety: AnxietyEntity
    ): StyledString {
        return ("<b>Anxiety</b> <code>#${anxiety.id}</code> ${resolution(anxiety.resolution)}" +
                "\n${anxiety.created.str(default)}" +
                "\n\n${anxiety.description}${details(anxiety)}")
            .styled(ParseMode.HTML)
    }

    private fun details(anxiety: AnxietyEntity): String {
        val trigger = anxiety.trigger ?: return ""
        val body = anxiety.bodyResponse ?: return ""
        val response = anxiety.anxietyResponse ?: return ""
        val alternative = anxiety.alternativeThoughts ?: return ""
        return "\n\n" +
                "<b><i>Trigger</i></b>: $trigger\n\n" +
                "<b><i>Body</i></b>: $body\n\n" +
                "<b><i>Response</i></b>: $response\n\n" +
                "<b><i>Alternative</i></b>: $alternative"
    }

    override fun deletedAnxiety(id: String): StyledString {
        return "*Anxiety* `#${id}` - `❌REMOVED`".styled(ParseMode.MARKDOWN)
    }

    override fun callbackAnswer(id: String): String {
        return "#${id} - ✅ Successfuly updated"
    }

    override fun resolution(resolution: AnxietyResolutionType): StyledString {
        return when (resolution) {
            AnxietyResolutionType.UNRESOLVED -> "\uD83D\uDD18"
            AnxietyResolutionType.FULFILLED -> "🔴"
            AnxietyResolutionType.UNFULFILLED -> "🟢"
        }.styled()
    }

    private fun formatTableResolution(resolution: AnxietyResolutionType): StyledString {
        return when (resolution) {
            AnxietyResolutionType.UNRESOLVED -> "▫\uFE0F"
            AnxietyResolutionType.FULFILLED -> "🔴"
            AnxietyResolutionType.UNFULFILLED -> "🟢"
        }.styled()
    }

}