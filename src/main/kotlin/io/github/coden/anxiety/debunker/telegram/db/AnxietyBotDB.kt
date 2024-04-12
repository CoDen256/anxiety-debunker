package io.github.coden.anxiety.debunker.telegram.db

import io.github.coden.telegram.db.BotDB
import io.github.coden.telegram.db.BotMessage
import io.github.coden.telegram.db.OwnerMessage
import io.github.coden.telegram.db.db
import io.github.coden.utils.notNullOrFailure
import org.telegram.abilitybots.api.db.MapDBContext
import java.io.Serializable

const val OWNER_ANXIETY_MESSAGES = "OWNER_ANXIETY_MESSAGES"
const val BOT_ANXIETY_MESSAGES = "BOT_ANXIETY_MESSAGES"

open class AnxietyBotDB(filename: String) :
    MapDBContext(db(filename)), BotDB {

    private val ownerMessages
        get() = getMap<OwnerMessage, String>(OWNER_ANXIETY_MESSAGES)

    private val botMessages
        get() = getSet<AnxietyLinkMessage>(BOT_ANXIETY_MESSAGES)

    fun addAnxietyToMessagesLink(anxietyId: String, inMessage: OwnerMessage, outMessage: BotMessage) {
        ownerMessages[inMessage] = anxietyId
        botMessages.add(AnxietyLinkMessage(outMessage, anxietyId))
        commit()
    }

    private fun addOwnerMessageLink(anxietyId: String, ownerMessage: OwnerMessage) {
        ownerMessages[ownerMessage] = anxietyId
        commit()
    }

    fun addBotMessageLink(anxietyId: String, ownerMessage: BotMessage) {
        botMessages.add(AnxietyLinkMessage(ownerMessage, anxietyId))
        commit()
    }

    fun getOwnerMessageByAnxiety(anxietyId: String): Result<OwnerMessage> {
        return ownerMessages
            .filterValues { it == anxietyId }
            .maxByOrNull { it.key.id }
            .notNullOrFailure(IllegalArgumentException("No owner message for this anxiety $anxietyId"))
            .map { it.key }
    }

    fun getBotMessagesByAnxiety(anxietyId: String): Set<BotMessage> {
        return botMessages
            .filter { it.anxietyId == anxietyId }
            .map { it.message }
            .toSet()
    }

    fun getAnxietyByOwnerMessage(ownerMessage: OwnerMessage): Result<String> {
        return ownerMessages[ownerMessage]
            .notNullOrFailure(IllegalArgumentException("Unable to find anxiety for $ownerMessage"))
    }

    fun getAnxietyByBotMessage(botMessage: BotMessage): Result<String> {
        return botMessages
            .firstOrNull { it.message == botMessage }
            ?.anxietyId
            .notNullOrFailure(IllegalArgumentException("Unable to find anxiety for $botMessage"))
    }

    fun deleteLinks(anxietyId: String): Result<Unit> {
        botMessages.removeIf { it.anxietyId == anxietyId }
        ownerMessages
            .filterValues { it == anxietyId }
            .toList()
            .forEach { ownerMessages.remove(it.first) }
        commit()
        return Result.success(Unit)
    }
}

data class AnxietyLinkMessage(
    val message: BotMessage,
    val anxietyId: String
): Serializable