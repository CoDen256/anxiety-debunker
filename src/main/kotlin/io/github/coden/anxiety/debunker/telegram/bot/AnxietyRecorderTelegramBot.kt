package io.github.coden.anxiety.debunker.telegram.bot

import io.github.coden.anxiety.debunker.core.api.*
import io.github.coden.anxiety.debunker.telegram.db.AnxietyBotDB
import io.github.coden.anxiety.debunker.telegram.formatter.AnxietyFormatter
import io.github.coden.telegram.abilities.*
import io.github.coden.telegram.db.OwnerMessage.Companion.asOwner
import io.github.coden.telegram.senders.send
import org.telegram.abilitybots.api.objects.Ability
import org.telegram.abilitybots.api.objects.Reply


class AnxietyRecorderTelegramBot(
    config: TelegramBotConfig,
    db: AnxietyBotDB,
    private val analyser: AnxietyAnalyser,
    private val holder: AnxietyHolder,
    private val resolver: AnxietyResolver,
    private val assessor: AnxietyAssessor,
    private val formatter: AnxietyFormatter,
    ) : AnxietyDebunkerTelegramBot(config, db, analyser, holder, resolver, assessor, formatter)
{

    override fun anxietyStats():Ability = ability("stat") { upd ->
        val anxieties = analyser
            .anxieties(ListAnxietiesRequest())
            .getOrThrow()

        val table = formatter.tableShort(anxieties)
        sender.send(table, upd.chat())
    }

    override fun onAllAnxieties() = ability("all") { upd ->
        analyser
            .anxieties(ListAnxietiesRequest())
            .getOrThrow()
            .anxieties
            .forEach { displayAnxietyAsMessage(it, upd) }
    }

    override fun onAnxiety(): Reply = replyOn({ isJustText(it) }) { upd ->
        silent.send("Damn it sucks \uD83D\uDE14\nBut I got you!", upd.chatId())

        val description = cleanText(upd)

        val newAnxiety = holder
            .add(NewAnxietyRequest(description))
            .getOrThrow()

        val resolution = AnxietyResolutionResponse(
            AnxietyResolutionType.UNRESOLVED,
            null
        )
        val response = formatter.anxiety(
            newAnxiety.id,
            newAnxiety.created,
            newAnxiety.description,
            resolution
        )
        val ownerMessage = upd.message.asOwner()
        val keyboard = keyboardFromResolution(resolution)
        val botMessage = sender
            .send(response, upd.chat(), keyboard)
        db().addAnxietyToMessagesLink(newAnxiety.id, ownerMessage, botMessage)
    }
}