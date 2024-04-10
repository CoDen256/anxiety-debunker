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
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.stream.Stream


class AnxietyDebunkerTelegramBot(
    private val config: TelegramBotConfig,
    private val analyser: AnxietyAnalyser,
    private val holder: AnxietyHolder,
    private val resolver: AnxietyResolver,
    private val assessor: AnxietyAssessor,
    private val formatter: AnxietyFormatter,
    private val anxietyDb: AnxietyDBContext,
) : AbilityBot(config.token, config.username, anxietyDb, options()), StartableLongPollingBot, Logging {
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
        val replyMarkup = withNewAnxietyButtons()
        val botMessage = sender
            .sendMd(response, getChatId(upd), replyMarkup, replyTo = owner?.id)
            .asBot()
        anxietyDb.addBotMessageLink(anxietyId, botMessage)
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
        val replyMarkup = withNewAnxietyButtons()
        val botMessage = sender
            .sendMd(response, getChatId(upd), replyMarkup)
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

        syncAnxietyMessages(updated.id, getChatId(upd))
    }

    fun onDeletedAnxiety(): Reply = replyOnReaction("\uD83D\uDC4E"){upd ->
        val anxietyId = anxietyDb
            .getAnxietyByBotMessage(upd.messageReaction.messageId.asBot())
            .getOrThrow()

        val deleted = holder.delete(DeleteAnxietyRequest(anxietyId)).getOrThrow()
        syncAnxietyMessages(deleted.id, getChatId(upd))
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

        val editBuilder = sender.editMdRequest(
            message,
            chatId,
            replyMarkup = markup,
        )

        for (target in targets.sortedByDescending { it.id }) {
            sender.execute(editBuilder.messageId(target.id).build())
        }
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
}