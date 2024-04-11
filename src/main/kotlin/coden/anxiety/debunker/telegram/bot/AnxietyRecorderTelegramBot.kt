package coden.anxiety.debunker.telegram.bot

import coden.anxiety.debunker.core.api.*
import coden.anxiety.debunker.telegram.TelegramBotConfig
import coden.anxiety.debunker.telegram.db.AnxietyDBContext
import coden.anxiety.debunker.telegram.db.BotMessage.Companion.asBot
import coden.anxiety.debunker.telegram.db.OwnerMessage.Companion.asOwner
import coden.anxiety.debunker.telegram.formatter.AnxietyFormatter
import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot
import org.telegram.telegrambots.abilitybots.api.objects.Ability
import org.telegram.telegrambots.abilitybots.api.objects.Reply
import org.telegram.telegrambots.abilitybots.api.util.AbilityUtils.getChatId
import org.telegram.telegrambots.meta.generics.TelegramClient

class AnxietyRecorderTelegramBot(
    private val config: TelegramBotConfig,
    private val analyser: AnxietyAnalyser,
    private val holder: AnxietyHolder,
    private val resolver: AnxietyResolver,
    private val assessor: AnxietyAssessor,
    private val formatter: AnxietyFormatter,
    private val anxietyDb: AnxietyDBContext,
    private val sender: TelegramClient,
    ) : AbilityBot(sender, config.username, anxietyDb),
    RunnableLongPollingBot {
    override fun creatorId(): Long {
        return config.target
    }

    override fun run() {
        silent.sendMd(config.intro, config.target)
    }

    override fun token(): String {
        return config.token
    }

    fun startCmd(): Ability = ability("start"){ run() }


    fun anxietyStats():Ability = ability("stat") { upd ->
        val anxieties = analyser
            .anxieties(ListAnxietiesRequest(AnxietyFilter.ALL))
            .getOrThrow()

        val table = formatter.formatShort(anxieties).asCodeSnippet()
        sender.sendHtml(table, getChatId(upd))
    }


    fun onAnxiety(): Reply = replyOn({ justText(it) }) { upd ->
        silent.send("Damn it sucks \uD83D\uDE14\nBut I got you!", upd.chatId())

        val description = cleanText(upd)

        val newAnxiety = holder
            .add(NewAnxietyRequest(description))
            .getOrThrow()


        val response = formatter.formatAnxiety(
            newAnxiety.id,
            newAnxiety.created,
            newAnxiety.description,
            AnxietyEntityResolution.UNRESOLVED
        )

        val ownerMessage = upd.message.asOwner()
        val botMessage = sender.sendMd(response, getChatId(upd)).asBot()
        anxietyDb.addAnxietyToMessagesLink(newAnxiety.id, ownerMessage, botMessage)
    }

}