package coden.anxiety.debunker.telegram.bot

import coden.anxiety.debunker.core.api.*
import coden.anxiety.debunker.telegram.TelegramBotConfig
import coden.anxiety.debunker.telegram.db.AnxietyDBContext
import coden.anxiety.debunker.telegram.db.BotMessage.Companion.asBot
import coden.anxiety.debunker.telegram.db.OwnerMessage.Companion.asOwner
import coden.anxiety.debunker.telegram.formatter.AnxietyFormatter
import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.util.AbilityUtils.getChatId
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

class AnxietyRecorderTelegramBot(
    private val config: TelegramBotConfig,
    private val analyser: AnxietyAnalyser,
    private val holder: AnxietyHolder,
    private val resolver: AnxietyResolver,
    private val assessor: AnxietyAssessor,
    private val formatter: AnxietyFormatter,
    private val anxietyDb: AnxietyDBContext,
) : AbilityBot(config.token, config.username, anxietyDb), StartableLongPollingBot {
    override fun creatorId(): Long {
        return config.target
    }

    override fun start() {
        silent.sendMd(config.intro, config.target)
    }

    fun startCmd() = ability("start"){ start() }


    fun anxietyStats() = ability("stat") { upd ->
        val anxieties = analyser
            .anxieties(ListAnxietiesRequest(AnxietyFilter.ALL))
            .getOrThrow()

        val s = "<pre>${formatter.formatShort(anxieties)}</pre>"
        val message = SendMessage().apply {
            text = s
            enableHtml(true)
            chatId = getChatId(upd).toString()
        }
         sender.execute(message)
    }

    fun onAnxiety() = replyOn({ justText(it) }) { upd ->
        silent.send("Damn it sucks \uD83D\uDE14\nBut I got you!", upd.chatId())

        val description = cleanText(upd)
        val newAnxiety = holder
            .add(NewAnxietyRequest(description))
            .getOrThrow()

        val message = SendMessage().apply {
            text = formatter.formatAnxiety(
                newAnxiety.id,
                newAnxiety.created,
                newAnxiety.description,
                AnxietyEntityResolution.UNRESOLVED
            )
            chatId = upd.strChatId()
            enableMarkdown(true)
        }

        val ownerMessage = upd.message.asOwner()
        val botMessage = sender.execute(message).asBot()
        anxietyDb.addOwnerMessage(newAnxiety.id, ownerMessage)
        anxietyDb.addBotMessage(newAnxiety.id, botMessage)
    }

}