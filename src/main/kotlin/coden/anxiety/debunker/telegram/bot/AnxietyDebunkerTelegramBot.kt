package coden.anxiety.debunker.telegram.bot

import coden.anxiety.debunker.core.api.*
import coden.anxiety.debunker.core.persistance.Chance
import coden.anxiety.debunker.telegram.TelegramBotConfig
import coden.anxiety.debunker.telegram.db.AnxietyDBContext
import coden.anxiety.debunker.telegram.db.BotMessage
import coden.anxiety.debunker.telegram.db.BotMessage.Companion.asBot
import coden.anxiety.debunker.telegram.db.OwnerMessage.Companion.asOwner
import coden.anxiety.debunker.telegram.formatter.AnxietyFormatter
import coden.utils.singleThreadScope
import kotlinx.coroutines.launch
import org.apache.logging.log4j.kotlin.Logging
import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot
import org.telegram.telegrambots.abilitybots.api.objects.Ability
import org.telegram.telegrambots.abilitybots.api.objects.Flag
import org.telegram.telegrambots.abilitybots.api.objects.Reply
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendSticker
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
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
) : AbilityBot(sender, config.username, anxietyDb), RunnableLongPollingBot, Logging {


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
            .anxieties(ListAnxietiesRequest(chances = ChanceFilter.HIGHEST_CHANCE))
            .getOrThrow()

        val table = formatter.formatTableWithResolutions(anxieties).asCodeSnippet()
        sender.sendHtml(table, it.chatId())
    }

    fun onAllAnxieties() = ability("all") { upd ->
        val anxieites = analyser
            .anxieties(ListAnxietiesRequest(chances = ChanceFilter.HIGHEST_CHANCE))
            .getOrThrow()
            .anxieties
            .forEach { displayAnxietyAsMessage(it, upd) }
    }

    fun onAnxietyId() = replyOn({ isId(it) }) { upd ->
        val anxietyId = getId(upd).getOrThrow()
        val anxiety = analyser.anxiety(GetAnxietyRequest(anxietyId)).getOrThrow()
        displayAnxietyAsMessage(anxiety, upd)
    }

    private fun displayAnxietyAsMessage(anxiety: AnxietyEntityResponse, upd: Update) {
        val response = formatter.formatAnxiety(
            anxiety.id,
            anxiety.created,
            anxiety.description,
            anxiety.resolution
        )

        val owner = anxietyDb.getOwnerMessageByAnxiety(anxiety.id).getOrNull()
        val replyMarkup = markupFromResolution(anxiety.resolution)
        val botMessage = sender
            .sendMd(response, upd.chatId(), replyMarkup, replyTo = owner?.id)
            .asBot()
        anxietyDb.addBotMessageLink(anxiety.id, botMessage)
    }

    fun onAnxiety(): Reply = replyOn({ justText(it) }) { upd ->
        silent.send("Damn it sucks \uD83D\uDE14\nBut I got you!", upd.chatId())

        val description = cleanText(upd)

        val newAnxiety = holder
            .add(NewAnxietyRequest(description))
            .getOrThrow()

        val resolution = AnxietyResolutionResponse(AnxietyResolutionType.UNRESOLVED, null)
        // property of the debunked bot. By default all of the HIGHEST level
        assessor
            .assess(NewChanceAssessmentRequest(ChanceAssessment.HIGHEST, newAnxiety.id))
            .getOrThrow()

        val response = formatter.formatAnxiety(
            newAnxiety.id,
            newAnxiety.created,
            newAnxiety.description,
            resolution
        )

        val ownerMessage = upd.message.asOwner()
        val replyMarkup = markupFromResolution(resolution)
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

    fun onDeletedAnxiety(): Reply = replyOnReaction("\uD83D\uDC4E") { upd ->
        val anxietyId = anxietyDb
            .getAnxietyByBotMessage(upd.messageReaction.messageId.asBot())
            .getOrThrow()

        val deleted = holder.delete(DeleteAnxietyRequest(anxietyId)).getOrThrow()

        syncAnxietyMessages(deleted.id, upd.chatId(), true)
    }

    fun onCallback(): Reply = replyOnCallback { update, data ->
        logger.info("Handling callback for $data")
        when (data) {
            FULFILL.data -> onResolve(update, true)
            UNFULFILL.data -> onResolve(update, false)
            UNRESOLVE.data -> onUnresolve(update)
            DELETE_MESSAGE.data -> onDeleteMessage(update)
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

        syncAnxietyMessages(result.anxietyId, update.chatId())
    }

    private fun onResolve(update: Update, fulfilled: Boolean) {
        val target = update.callbackQuery.message.asBot()
        val anxiety = anxietyDb
            .getAnxietyByBotMessage(target)
            .getOrThrow()

        if (!fulfilled){
            sender.sendMd("\uD83C\uDF89", update.chatId())
        }

        val result = resolver
            .resolve(ResolveAnxietyRequest(anxiety, fulfilled))
            .getOrThrow()

        sender.execute(
            AnswerCallbackQuery.builder().apply {
                callbackQueryId(update.callbackQuery.id)
                text(formatter.formatUpdatedAnxiety(result.anxietyId))
            }.build(),
        )
        singleThreadScope.launch {
            syncAnxietyMessages(result.anxietyId, update.chatId())
        }
    }

    private fun onDeleteMessage(update: Update){
        val anxietyId = anxietyDb
            .getAnxietyByBotMessage(update.callbackQuery.message.messageId.asBot())
            .getOrThrow()
        val targets = anxietyDb.getBotMessagesByAnxiety(anxietyId)
        anxietyDb.deleteLinks(anxietyId)

        for (target in targets) {
            sender.execute(DeleteMessage.builder().apply {
                messageId(target.id)
                chatId(update.chatId())
            }.build())
        }
    }


    private fun syncAnxietyMessages(anxietyId: String, chatId: Long, deleted: Boolean=false) {
        val targets: Set<BotMessage> = anxietyDb.getBotMessagesByAnxiety(anxietyId)

        if (deleted){
            return markDeleted(anxietyId, chatId, targets)
        }

        val anxiety = analyser
            .anxiety(GetAnxietyRequest(anxietyId))
            .getOrThrow()

        val markup = markupFromResolution(anxiety.resolution)

        val message = formatter.formatAnxiety(
            anxiety.id,
            anxiety.created,
            anxiety.description,
            anxiety.resolution
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

    private fun markupFromResolution(resolution: AnxietyResolutionResponse): InlineKeyboardMarkup {
        val markup = when (resolution.type) {
            AnxietyResolutionType.UNRESOLVED -> withNewAnxietyButtons()
            else -> withResolvedAnxietyButtons()
        }
        return markup
    }

    private fun markDeleted(
        anxietyId: String,
        chatId: Long,
        targets: Set<BotMessage>
    ) {
        for (target in targets.sortedByDescending { it.id }) {
            sender.editMd(
                target.id,
                formatter.formatDeletedAnxiety(anxietyId),
                chatId,
                replyMarkup = withDeletedAnxietyButtons()
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