package io.github.coden.anxiety.debunker.telegram.bot

import io.github.coden.anxiety.debunker.core.api.*
import io.github.coden.anxiety.debunker.telegram.db.AnxietyBotDB
import io.github.coden.anxiety.debunker.telegram.formatter.AnxietyEntity
import io.github.coden.anxiety.debunker.telegram.formatter.AnxietyFormatter
import io.github.coden.telegram.abilities.*
import io.github.coden.telegram.db.BotMessage
import io.github.coden.telegram.db.BotMessage.Companion.asBot
import io.github.coden.telegram.db.Chat
import io.github.coden.telegram.db.OwnerMessage.Companion.asOwner
import io.github.coden.telegram.keyboard.Keyboard
import io.github.coden.telegram.senders.*
import io.github.coden.utils.recover
import io.github.coden.utils.singleThreadScope
import kotlinx.coroutines.launch
import org.telegram.abilitybots.api.objects.Ability
import org.telegram.abilitybots.api.objects.Flag
import org.telegram.abilitybots.api.objects.Reply
import org.telegram.telegrambots.meta.api.objects.Update
import kotlin.random.Random

// TODO Reload list on edited
// TODO Severity
class AnxietyDebunkerTelegramBot(
    config: TelegramBotConfig,
    db: AnxietyBotDB,
    private val analyser: AnxietyAnalyser,
    private val holder: AnxietyHolder,
    private val resolver: AnxietyResolver,
    private val assessor: AnxietyAssessor,
    private val detailEditor: AnxietyDetailEditor,
    private val formatter: AnxietyFormatter,
) : BaseTelegramBot<AnxietyBotDB>(config, db) {


    fun anxietyStatsVerbose(): Ability = ability("list") { upd ->
        formatAndSendAnxieties(upd, ChanceFilter.ALL) { formatter.listVerbose(it) }
    }

    fun anxietyStats(): Ability = ability("lis") { upd ->
        formatAndSendAnxieties(upd, ChanceFilter.ALL) { formatter.list(it) }
    }

    fun anxietyStatsConcise(): Ability = ability("ls") { upd ->
        formatAndSendAnxieties(upd, ChanceFilter.ALL) { formatter.listConcise(it) }
    }

    fun anxietyStatsVeryConcise(): Ability = ability("l") { upd ->
        formatAndSendAnxieties(upd, ChanceFilter.ALL) { formatter.listVeryConcise(it) }
    }

    fun anxietyStatsTable(): Ability = ability("table") { upd ->
        formatAndSendAnxieties(upd, ChanceFilter.ALL) { formatter.table(it) }
    }

    fun anxietyStatsTableConcise(): Ability = ability("tb") { upd ->
        formatAndSendAnxieties(upd, ChanceFilter.ALL) { formatter.tableConcise(it) }
    }

    private fun formatAndSendAnxieties(
        update: Update,
        filter: ChanceFilter,
        format: (AnxietyListResponse) -> (StyledString)
    ) {
        val anxieties = analyser
            .anxieties(ListAnxietiesRequest(chances = filter))
            .getOrThrow()

        val table = format(anxieties)
        sender.send(table, update.chat())
    }


    fun onAllAnxieties(): Ability = ability("all") { upd ->
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

    private fun displayAnxietyAsMessage(anxiety: AnxietyEntityResponse, upd: Update) {
        val response = formatter.anxiety(AnxietyEntity(
            anxiety.id,
            anxiety.created,
            anxiety.description,
            anxiety.resolution.type,
            anxiety.details?.trigger,
            anxiety.details?.bodyResponse,
            anxiety.details?.anxietyResponse,
            anxiety.details?.alternativeThoughts
        ))

        val ownerMessage = db().getOwnerMessageByAnxiety(anxiety.id).getOrNull()
        val botMessage = sender
            .send(response, upd.chat(), keyboardFromResolution(anxiety.resolution.type), ownerMessage)

        db().addBotMessageLink(anxiety.id, botMessage)
    }

    fun onAnxiety(): Reply = replyOn({ isJustText(it) }) { upd ->
        silent.send("Damn it sucks \uD83D\uDE14\nBut I got you!", upd.chatId())

        val message = cleanText(upd)
        val anxiety = extractAnxietyMessage(message)

        val newAnxiety = holder
            .add(NewAnxietyRequest(anxiety.description))
            .getOrThrow()

        // property of the debunked bot. By default all of the HIGHEST level
        assessor
            .assess(NewChanceAssessmentRequest(ChanceAssessment.HIGHEST, newAnxiety.id))
            .onFailure { sender.send("Unable to add assessment ${newAnxiety.id}.", upd.chat()) }

        val details = anxiety.details?.let {
            detailEditor
                .add(
                    NewDetailRequest(
                        newAnxiety.id,
                        it.trigger,
                        it.bodyResponse,
                        it.anxietyResponse,
                        it.alternativeThoughts
                    )
                )
                .onFailure { sender.send("Unable to add details to ${newAnxiety.id}.", upd.chat()) }
                .getOrNull()
        }

        val defaultResolution = AnxietyResolutionType.UNRESOLVED

        val response = formatter.anxiety(
            AnxietyEntity(
                newAnxiety.id,
                newAnxiety.created,
                newAnxiety.description,
                defaultResolution,
                details?.trigger,
                details?.bodyResponse,
                details?.anxietyResponse,
                details?.alternativeThoughts
            )
        )

        val ownerMessage = upd.message.asOwner()
        val botMessage = sender.send(response, upd.chat(), keyboardFromResolution(defaultResolution))
        db().addAnxietyToMessagesLink(newAnxiety.id, ownerMessage, botMessage)
    }

    data class AnxietyMessage(val description: String, val details: AnxietyDetails?)
    data class AnxietyDetails(
        val trigger: String,
        val bodyResponse: String,
        val anxietyResponse: String,
        val alternativeThoughts: String
    )

    private val detailsRegex = Regex("T\\. (.+)\\nB\\. (.+)\\nR\\. (.+)\\nA\\. (.+)")
    private fun extractAnxietyMessage(message: String): AnxietyMessage {
        val split = message.split("\n\n\n")
        val description = split[0]

        if (split.size == 1) return AnxietyMessage(description,null)
        return AnxietyMessage(description, extractDetails(split[1]))
    }

    private fun extractDetails(message: String): AnxietyDetails? {
        val match = detailsRegex.find(message)
        return if (match != null) {
            AnxietyDetails(
                trigger = match.groups[1]?.value?.trim() ?: return null,
                bodyResponse = match.groups[2]?.value?.trim() ?: return null,
                anxietyResponse = match.groups[3]?.value?.trim()?: return null,
                alternativeThoughts = match.groups[4]?.value?.trim() ?: return null,
            )
        } else { null }
    }

    fun onEditedAnxiety(): Reply = replyOn(Flag.EDITED_MESSAGE) { upd ->
        val anxiety = db()
            .getAnxietyByOwnerMessage(upd.editedMessage.asOwner())
            .getOrThrow()

        val message = cleanText(upd.editedMessage)
        val new = extractAnxietyMessage(message)

        val updated = holder
            .update(UpdateAnxietyRequest(anxiety, new.description))
            .getOrThrow()

        new.details?.let { new ->
            detailEditor
                .update(UpdateDetailRequest(anxiety, new.trigger, new.bodyResponse, new.anxietyResponse, new.alternativeThoughts))
                .onFailure { detailEditor.add(NewDetailRequest(anxiety, new.trigger, new.bodyResponse, new.anxietyResponse, new.alternativeThoughts))
                    .onFailure { sender.send("Cannot add or update the details of #${anxiety}", upd.chat()) }
                }
        } ?: detailEditor
            .remove(DeleteDetailRequest(anxiety))
            .onFailure { sender.send("Cannot delete the details of #${anxiety}", upd.chat()) }

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

        if (!fulfilled and (Random.nextInt(0, 4) == 0)) {
            sender.send("\uD83C\uDF89", update.chat())
        }

        val result = resolver
            .resolve(ResolveAnxietyRequest(anxiety, fulfilled))
            .getOrThrow()

        sender.answerCallback(update, formatter.callbackAnswer(result.anxietyId))

        singleThreadScope.launch {
            syncAnxietyMessages(result.anxietyId, update.chat())
        }
    }

    private fun onDeleteMessage(update: Update) {
        val anxietyId = db()
            .getAnxietyByBotMessage(update.callbackQuery.message.messageId.asBot())
            .getOrThrow()
        val targets = db().getBotMessagesByAnxiety(anxietyId)
        db().deleteLinks(anxietyId)

        for (target in targets) {
            sender.deleteMessage(target, update.chat())
        }
    }


    private fun syncAnxietyMessages(anxietyId: String, chat: Chat, deleted: Boolean = false) {
        val targets: Set<BotMessage> = db().getBotMessagesByAnxiety(anxietyId)

        if (deleted) {
            return markDeleted(anxietyId, chat, targets)
        }

        val anxiety = analyser
            .anxiety(GetAnxietyRequest(anxietyId))
            .getOrNull()
            ?: return markDeleted(anxietyId, chat, targets)

        val keyboard = keyboardFromResolution(anxiety.resolution.type)

        val message = formatter.anxiety(AnxietyEntity(
            anxiety.id,
            anxiety.created,
            anxiety.description,
            anxiety.resolution.type,
            anxiety.details?.trigger,
            anxiety.details?.bodyResponse,
            anxiety.details?.anxietyResponse,
            anxiety.details?.alternativeThoughts,
        ))

        val editBuilder = sender.editBuilder(
            message,
            chat,
            keyboard,
        )

        for (target in targets.sortedByDescending { it.id }) {
            sender.execute(editBuilder.messageId(target.id).build())
        }
    }

    private fun keyboardFromResolution(anxietyResolutionType: AnxietyResolutionType): Keyboard {
        val markup = when (anxietyResolutionType) {
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