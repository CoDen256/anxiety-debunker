package io.github.coden.anxiety.debunker.telegram.bot

import io.github.coden.anxiety.debunker.core.api.*
import io.github.coden.anxiety.debunker.telegram.db.AnxietyBotDB
import io.github.coden.anxiety.debunker.telegram.formatter.AnxietyFormatter
import io.github.coden.telegram.abilities.*
import io.github.coden.telegram.db.BotMessage
import io.github.coden.telegram.db.BotMessage.Companion.asBot
import io.github.coden.telegram.db.Chat
import io.github.coden.telegram.db.OwnerMessage.Companion.asOwner
import io.github.coden.telegram.keyboard.Keyboard
import io.github.coden.telegram.senders.*
import io.github.coden.utils.singleThreadScope
import kotlinx.coroutines.launch
import org.telegram.abilitybots.api.objects.Ability
import org.telegram.abilitybots.api.objects.Flag
import org.telegram.abilitybots.api.objects.Reply
import org.telegram.telegrambots.meta.api.objects.Update
import kotlin.random.Random

// TODO Reload list on edited
// TODO Severity
open class AnxietyDebunkerTelegramBot(
    config: TelegramBotConfig,
    db: AnxietyBotDB,
    private val analyser: AnxietyAnalyser,
    private val holder: AnxietyHolder,
    private val resolver: AnxietyResolver,
    private val assessor: AnxietyAssessor,
    private val formatter: AnxietyFormatter,
) : BaseTelegramBot<AnxietyBotDB>(config, db){

    open fun anxietyStats(): Ability = ability("stat") {
        val anxieties = analyser
            .anxieties(ListAnxietiesRequest(chances = ChanceFilter.HIGHEST_CHANCE))
            .getOrThrow()

        val table = formatter.tableWithResolutions(anxieties)
        sender.send(table, it.chat())
    }


    open fun onAllAnxieties(): Ability = ability("all") { upd ->
        analyser
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

    protected fun displayAnxietyAsMessage(anxiety: AnxietyEntityResponse, upd: Update) {
        val response = formatter.anxiety(
            anxiety.id,
            anxiety.created,
            anxiety.description,
            anxiety.resolution
        )

        val ownerMessage = db().getOwnerMessageByAnxiety(anxiety.id).getOrNull()
        val keyboard = keyboardFromResolution(anxiety.resolution)
        val botMessage = sender
            .send(response, upd.chat(), keyboard, ownerMessage)

        db().addBotMessageLink(anxiety.id, botMessage)
    }

    open fun onAnxiety(): Reply = replyOn({ isJustText(it) }) { upd ->
        silent.send("Damn it sucks \uD83D\uDE14\nBut I got you!", upd.chatId())

        val description = cleanText(upd)

        val newAnxiety = holder
            .add(NewAnxietyRequest(description))
            .getOrThrow()

        val resolution = AnxietyResolutionResponse(
            AnxietyResolutionType.UNRESOLVED,
            null
        )
        // property of the debunked bot. By default all of the HIGHEST level
        assessor
            .assess(NewChanceAssessmentRequest(ChanceAssessment.HIGHEST, newAnxiety.id))
            .getOrThrow()

        val response = formatter.anxiety(
            newAnxiety.id,
            newAnxiety.created,
            newAnxiety.description,
            resolution
        )

        val ownerMessage = upd.message.asOwner()
        val keyboard = keyboardFromResolution(resolution)
        val botMessage = sender.send(response, upd.chat(), keyboard)
        db().addAnxietyToMessagesLink(newAnxiety.id, ownerMessage, botMessage)
    }


    fun onEditedAnxiety(): Reply = replyOn(Flag.EDITED_MESSAGE) { upd ->
        val anxiety = db()
            .getAnxietyByOwnerMessage(upd.editedMessage.asOwner())
            .getOrThrow()

        val updated = holder
            .update(UpdateAnxietyRequest(anxiety, upd.editedMessage.text))
            .getOrThrow()

        syncAnxietyMessages(updated.id, upd.chat())
    }

    fun onDeletedAnxiety(): Reply = replyOnReaction("\uD83D\uDC4E") { upd ->
        val anxietyId = db()
            .getAnxietyByBotMessage(upd.messageReaction.messageId.asBot())
            .getOrThrow()

        val deleted = holder.delete(DeleteAnxietyRequest(anxietyId)).getOrThrow()

        syncAnxietyMessages(deleted.id, upd.chat(), true)
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
        val anxiety = db()
            .getAnxietyByBotMessage(target)
            .getOrThrow()

        val result = resolver
            .unresolve(UnresolveAnxietyRequest(anxiety))
            .getOrThrow()

        syncAnxietyMessages(result.anxietyId, update.chat())
    }

    private fun onResolve(update: Update, fulfilled: Boolean) {
        val target = update.callbackQuery.message.asBot()
        val anxiety = db()
            .getAnxietyByBotMessage(target)
            .getOrThrow()

        if (!fulfilled and (Random.nextInt(0, 4) == 0)){
            sender.send("\uD83C\uDF89", update.chat())
        }

        val result = resolver
            .resolve(ResolveAnxietyRequest(anxiety, fulfilled))
            .getOrThrow()

        sender.answerCallback(update, formatter.callbackAnswer(result.anxietyId).toString())

        singleThreadScope.launch {
            syncAnxietyMessages(result.anxietyId, update.chat())
        }
    }

    private fun onDeleteMessage(update: Update){
        val anxietyId = db()
            .getAnxietyByBotMessage(update.callbackQuery.message.messageId.asBot())
            .getOrThrow()
        val targets = db().getBotMessagesByAnxiety(anxietyId)
        db().deleteLinks(anxietyId)

        for (target in targets) {
            sender.deleteMessage(target, update.chat())
        }
    }


    private fun syncAnxietyMessages(anxietyId: String, chat: Chat, deleted: Boolean=false) {
        val targets: Set<BotMessage> = db().getBotMessagesByAnxiety(anxietyId)

        if (deleted){
            return markDeleted(anxietyId, chat, targets)
        }

        val anxiety = analyser
            .anxiety(GetAnxietyRequest(anxietyId))
            .getOrNull()
            ?: return markDeleted(anxietyId, chat, targets)

        val keyboard = keyboardFromResolution(anxiety.resolution)

        val message = formatter.anxiety(
            anxiety.id,
            anxiety.created,
            anxiety.description,
            anxiety.resolution
        )

        val editBuilder = sender.editBuilder(
            message,
            chat,
            keyboard,
        )

        for (target in targets.sortedByDescending { it.id }) {
            sender.execute(editBuilder.messageId(target.id).build())
        }
    }

    protected fun keyboardFromResolution(resolution: AnxietyResolutionResponse): Keyboard {
        val markup = when (resolution.type) {
            AnxietyResolutionType.UNRESOLVED -> withNewAnxietyButtons()
            else -> withResolvedAnxietyButtons()
        }
        return markup
    }

    private fun markDeleted(
        anxietyId: String,
        chat: Chat,
        targets: Set<BotMessage>
    ) {
        for (target in targets.sortedByDescending { it.id }) {
            sender.edit(
                formatter.deletedAnxiety(anxietyId),
                chat,
                target,
                withDeletedAnxietyButtons()
            )
        }
    }
}