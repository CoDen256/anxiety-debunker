package coden.anxiety.debunker.telegram.bot

import coden.anxiety.debunker.core.api.*
import coden.anxiety.debunker.core.persistance.RiskLevel
import coden.anxiety.debunker.telegram.TelegramBotConfig
import coden.anxiety.debunker.telegram.db.AnxietyDBContext
import coden.anxiety.debunker.telegram.db.BotMessage
import coden.anxiety.debunker.telegram.db.BotMessage.Companion.asBot
import coden.anxiety.debunker.telegram.db.OwnerMessage.Companion.asOwner
import coden.anxiety.debunker.telegram.formatter.AnxietyFormatter
import org.apache.logging.log4j.kotlin.Logging
import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot
import org.telegram.telegrambots.abilitybots.api.objects.Ability
import org.telegram.telegrambots.abilitybots.api.objects.Flag
import org.telegram.telegrambots.abilitybots.api.objects.Reply
import org.telegram.telegrambots.abilitybots.api.util.AbilityUtils.getChatId
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.business.BusinessMessagesDeleted
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.generics.TelegramClient


class AnxietyDebunkerTelegramBot(
    private val config: TelegramBotConfig,
    private val analyser: AnxietyAnalyser,
    private val holder: AnxietyHolder,
    private val resolver: AnxietyResolver,
    private val assessor: AnxietyAssessor,
    private val formatter: AnxietyFormatter,
    private val anxietyDb: AnxietyDBContext,
    private val sender: TelegramClient,
) : AbilityBot(sender, config.username, anxietyDb), StartableLongPollingBot, Logging {

    init {
        onRegister()
    }



    override fun creatorId(): Long {
        return config.target
    }

    override fun run() {
        silent.sendMd(config.intro, config.target)
    }

    override fun token(): String {
        return config.token
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

    fun onAnxietyId() = replyOn({ isId(it)}){ upd ->
        val anxietyId = getId(upd).getOrThrow()

        val anxiety = analyser.anxiety(AnxietyRequest(anxietyId)).getOrThrow()
        val response = formatter.formatAnxiety(
            anxiety.id,
            anxiety.created,
            anxiety.description,
            anxiety.resolution
        )

        val owner = anxietyDb.getOwnerMessageByAnxiety(anxietyId).getOrNull()
        val replyMarkup = markupFromResolution(anxiety.resolution)
        val botMessage = sender
            .sendMd(response, upd.chatId(), replyMarkup, replyTo = owner?.id)
            .asBot()
        anxietyDb.addBotMessageLink(anxietyId, botMessage)
    }

    fun onAnxiety(): Reply = replyOn({ justText(it) }) { upd ->
        silent.send("Gotcha", upd.chatId())

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
        val replyMarkup = withNewAnxietyButtons()
        val botMessage = sender
            .sendMd(response, upd.chatId(), replyMarkup)
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

        syncAnxietyMessages(updated.id, upd.chatId())
    }

    fun onDeletedAnxiety(): Reply = replyOnReaction("\uD83D\uDC4E"){upd ->
        val anxietyId = anxietyDb
            .getAnxietyByBotMessage(upd.messageReaction.messageId.asBot())
            .getOrThrow()

        val deleted = holder.delete(DeleteAnxietyRequest(anxietyId)).getOrThrow()
        syncAnxietyMessages(deleted.id, upd.chatId())
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

        syncAnxietyMessages(result.anxietyId, getChatId(update))
    }

    private fun onResolve(update: Update, fulfilled: Boolean) {
        val target = update.callbackQuery.message.asBot()
        val anxiety = anxietyDb
            .getAnxietyByBotMessage(target)
            .getOrThrow()

        val result = resolver
            .resolve(ResolveAnxietyRequest(anxiety, fulfilled))
            .getOrThrow()

        syncAnxietyMessages(result.anxietyId, getChatId(update))
    }


    private fun syncAnxietyMessages(
        anxietyId: String,
        chatId: Long,
    ) {
        val targets: Set<BotMessage> = anxietyDb.getBotMessagesByAnxiety(anxietyId)

        val updatedAnxiety = analyser
            .anxiety(AnxietyRequest(anxietyId))
            .getOrNull()
            ?: return markDeleted(anxietyId, chatId, targets)


        val markup = markupFromResolution(updatedAnxiety.resolution)

        val message = formatter.formatAnxiety(
            updatedAnxiety.id,
            updatedAnxiety.created,
            updatedAnxiety.description,
            updatedAnxiety.resolution
        )

        val editBuilder = sender.editMdRequest(
            message,
            chatId,
            replyMarkup = markup,
        )

        for (target in targets.sortedByDescending { it.id }) {
            sender.execute(editBuilder.messageId(target.id).build())
        }
    }

    private fun markupFromResolution(
        resolution: AnxietyEntityResolution
                                     ): InlineKeyboardMarkup {
        val markup = when (resolution) {
            AnxietyEntityResolution.UNRESOLVED -> withNewAnxietyButtons()
            else -> withResolvedAnxietyButtons()
        }
        return markup
    }

    private fun markDeleted(anxietyId: String,
                            chatId: Long,
                            targets: Set<BotMessage>){
        for (target in targets.sortedByDescending { it.id }) {
            sender.editMd(
                target.id,
                formatter.formatDeletedAnxiety(anxietyId),
                chatId,
                replyMarkup = null
            )
        }
    }

    override fun consume(update: Update?) {
        // library does not see a valid user on reactions
        // hack to force library to think it has a valid user, but supplying fake info
        // it'll return EMPTY_USER
        update?.deletedBusinessMessages = BusinessMessagesDeleted()
        super.consume(update)
    }
}