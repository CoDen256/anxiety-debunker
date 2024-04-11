package coden.anxiety.debunker.telegram.bot

import coden.anxiety.debunker.core.api.*
import coden.anxiety.debunker.telegram.TelegramBotConfig
import coden.anxiety.debunker.telegram.db.AnxietyDBContext
import coden.anxiety.debunker.telegram.db.BotMessage
import coden.anxiety.debunker.telegram.db.BotMessage.Companion.asBot
import coden.anxiety.debunker.telegram.db.OwnerMessage.Companion.asOwner
import coden.anxiety.debunker.telegram.formatter.AnxietyFormatter
import org.apache.logging.log4j.kotlin.Logging
import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.objects.Ability
import org.telegram.abilitybots.api.objects.Flag
import org.telegram.abilitybots.api.objects.Reply
import org.telegram.abilitybots.api.util.AbilityUtils.EMPTY_USER
import org.telegram.abilitybots.api.util.AbilityUtils.getChatId
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.ChatJoinRequest
import org.telegram.telegrambots.meta.api.objects.Update


class AnxietyRecorderTelegramBot(
    private val config: TelegramBotConfig,
    private val analyser: AnxietyAnalyser,
    private val holder: AnxietyHolder,
    private val resolver: AnxietyResolver,
    private val assessor: AnxietyAssessor,
    private val formatter: AnxietyFormatter,
    private val anxietyDb: AnxietyDBContext,
    ) : AbilityBot(config.token, config.username, anxietyDb, options()),
    RunnableLongPollingBot, Logging {
    override fun creatorId(): Long {
        return config.target
    }

    override fun run() {
        silent.sendMd(config.intro, config.target)
    }

    fun startCmd(): Ability = ability("start"){ run() }


    fun anxietyStats():Ability = ability("stat") { upd ->
        val anxieties = analyser
            .anxieties(ListAnxietiesRequest())
            .getOrThrow()

        val table = formatter.formatTableShort(anxieties).asCodeSnippet()
        sender.sendHtml(table, getChatId(upd))
    }

    fun onAllAnxieties() = ability("all") { upd ->
        analyser
            .anxieties(ListAnxietiesRequest())
            .getOrThrow()
            .anxieties
            .forEach { displayAnxietyAsMessage(it, upd) }
    }
    fun onAnxietyId() = replyOn({ isId(it) }) { upd ->
        val anxietyId = getId(upd).getOrThrow()
        val anxiety = analyser.anxiety(GetAnxietyRequest(anxietyId)).getOrThrow()
        displayAnxietyAsMessage(anxiety, upd)
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

    fun onAnxiety(): Reply = replyOn({ justText(it) }) { upd ->
        silent.send("Damn it sucks \uD83D\uDE14\nBut I got you!", upd.chatId())

        val description = cleanText(upd)

        val newAnxiety = holder
            .add(NewAnxietyRequest(description))
            .getOrThrow()

        val resolution = AnxietyResolutionResponse(AnxietyResolutionType.UNRESOLVED, null)
        val response = formatter.formatAnxiety(
            newAnxiety.id,
            newAnxiety.created,
            newAnxiety.description,
            resolution
        )

        val ownerMessage = upd.message.asOwner()
        val botMessage = sender.sendMd(response, getChatId(upd)).asBot()
        anxietyDb.addAnxietyToMessagesLink(newAnxiety.id, ownerMessage, botMessage)
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
            DELETE_MESSAGE.data -> onDeleteMessage(update)
        }
    }

    private fun displayAnxietyAsMessage(anxiety: AnxietyEntityResponse, upd: Update) {
        val response = formatter.formatAnxiety(
            anxiety.id,
            anxiety.created,
            anxiety.description,
            anxiety.resolution
        )

        val owner = anxietyDb.getOwnerMessageByAnxiety(anxiety.id).getOrNull()
        val botMessage = sender
            .sendMd(response, upd.chatId(), replyTo = owner?.id)
            .asBot()
        anxietyDb.addBotMessageLink(anxiety.id, botMessage)
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

        val message = formatter.formatAnxiety(
            anxiety.id,
            anxiety.created,
            anxiety.description,
            anxiety.resolution
        )

        val editBuilder = sender.editMdRequest(
            message,
            chatId,
        )

        for (target in targets.sortedByDescending { it.id }) {
            sender.execute(editBuilder.messageId(target.id).build())
        }
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

    override fun onUpdateReceived(update: Update?) {
        // library does not see a valid user on reactions
        // hack to force library to think it has a valid user, but supplying fake info
        // it'll return EMPTY_USER
        if (update?.messageReaction != null){
            update.chatJoinRequest = ChatJoinRequest().apply { user=EMPTY_USER }
        }
        super.onUpdateReceived(update)
    }
}