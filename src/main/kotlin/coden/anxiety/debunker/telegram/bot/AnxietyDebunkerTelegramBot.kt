package coden.anxiety.debunker.telegram.bot

import coden.anxiety.debunker.core.api.*
import coden.anxiety.debunker.core.persistance.RiskLevel
import coden.anxiety.debunker.telegram.*
import coden.anxiety.debunker.telegram.db.AnxietyDBContext
import coden.anxiety.debunker.telegram.db.BotMessage
import coden.anxiety.debunker.telegram.db.BotMessage.Companion.asBot
import coden.anxiety.debunker.telegram.db.OwnerMessage.Companion.asOwner
import coden.anxiety.debunker.telegram.formatter.AnxietyFormatter
import org.apache.logging.log4j.kotlin.Logging
import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.objects.*
import org.telegram.abilitybots.api.util.AbilityUtils.getChatId
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.objects.Update


class AnxietyDebunkerTelegramBot(
    private val config: TelegramBotConfig,
    private val analyser: AnxietyAnalyser,
    private val holder: AnxietyHolder,
    private val resolver: AnxietyResolver,
    private val assessor: AnxietyAssessor,
    private val formatter: AnxietyFormatter,
    private val anxietyDb: AnxietyDBContext,
) : AbilityBot(config.token, config.username, anxietyDb), StartableLongPollingBot, Logging {
    override fun creatorId(): Long {
        return config.target
    }

    override fun run() {
        silent.sendMd(config.intro, config.target)
    }

    fun start(): Ability = ability("start") {
        run()
    }

    fun anxietyStats(): Ability = ability("stat") {
        val anxieties = analyser
            .anxieties(ListAnxietiesRequest(AnxietyFilter.MAX_RISK))
            .getOrThrow()

        val table = formatter.format(anxieties).asCodeSnippet()
        sender.sendHtml(table, getChatId(it))
    }

    fun onAnxiety(): Reply = replyOn({ justText(it) }) { upd ->
        silent.send("Gotcha", getChatId(upd))

        val description = cleanText(upd)

        val newAnxiety = holder
            .add(NewAnxietyRequest(description))
            .getOrThrow()

        // property of the debunked bot. By default all of MAX level
        assessor
            .add(NewRiskRequest(RiskLevel.MAX, newAnxiety.id))
            .getOrThrow()

        val response = formatter.formatAnxiety(
            newAnxiety.id,
            newAnxiety.created,
            newAnxiety.description,
            AnxietyEntityResolution.UNRESOLVED
        )

        val ownerMessage = upd.message.asOwner()
        val botMessage = sender
            .sendMd(response, getChatId(upd), withNewAnxietyButtons())
            .asBot()
        anxietyDb.addAnxietyToMessagesLink(newAnxiety.id, ownerMessage, botMessage)
    }

    fun onEditedAnxiety(): Reply = replyOn(Flag.EDITED_MESSAGE) { upd ->
        val anxiety = anxietyDb
            .getAnxietyByOwnerMessage(upd.editedMessage.asOwner())
            .getOrThrow()

        val updated = holder
            .update(UpdateAnxietyRequest(anxiety, upd.editedMessage.text))
            .getOrThrow()

        val targets: Set<BotMessage> = anxietyDb.getBotMessagesByAnxiety(anxiety)
        for (target in targets) {
            updateAnxietyMessage(target, updated.id, getChatId(upd))
        }
    }

    fun onCallback(): Reply = replyOnCallback { update, data ->
        logger.info("Handling callback for $data")
        when (data) {
            FULFILL.data -> onResolve(update, true)
            UNFULFILL.data -> onResolve(update, false)
            UNRESOLVE.data -> onUnresolve(update)
        }
    }

    private fun onUnresolve(update: Update) {
        val target = update.callbackQuery.message.asBot()
        val anxiety = anxietyDb
            .getAnxietyByBotMessage(target)
            .getOrThrow()

        val result = resolver
            .unresolve(UnresolveAnxietyRequest(anxiety))
            .getOrThrow()

        updateReplyMarkup(target, result.anxietyId, getChatId(update))
        updateAnxietyMessage(target, result.anxietyId, getChatId(update))
    }


    private fun onResolve(update: Update, fulfilled: Boolean) {
        val target = update.callbackQuery.message.asBot()
        val anxiety = anxietyDb
            .getAnxietyByBotMessage(target)
            .getOrThrow()

        val result = resolver
            .resolve(ResolveAnxietyRequest(anxiety, fulfilled))
            .getOrThrow()

        updateReplyMarkup(target, result.anxietyId, getChatId(update))
        updateAnxietyMessage(target, result.anxietyId, getChatId(update))
    }

    private fun updateReplyMarkup(
        target: BotMessage,
        anxietyId: String,
        chatId: Long
    ) {
        val anxietyEntity = analyser.anxiety(AnxietyRequest(anxietyId))
            .getOrThrow()

        val markup = when (anxietyEntity.resolution) {
            AnxietyEntityResolution.UNRESOLVED -> withNewAnxietyButtons()
            else -> withResolvedAnxietyButtons()
        }
        val edit = EditMessageReplyMarkup()

        edit.replyMarkup = markup
        edit.messageId = target.id
        edit.chatId = chatId.toString()

        sender.execute(edit)
    }

    private fun updateAnxietyMessage(
        target: BotMessage,
        anxietyId: String,
        chatId: Long
    ) {
        val updatedAnxiety = analyser
            .anxiety(AnxietyRequest(anxietyId))
            .getOrThrow()

        val markup = when (updatedAnxiety.resolution) {
            AnxietyEntityResolution.UNRESOLVED -> withNewAnxietyButtons()
            else -> withResolvedAnxietyButtons()
        }

        val message = formatter.formatAnxiety(
            updatedAnxiety.id,
            updatedAnxiety.created,
            updatedAnxiety.description,
            updatedAnxiety.resolution
        )
        sender.editMd(
            target.id,
            message,
            chatId,
            replyMarkup = markup
        )
    }
}