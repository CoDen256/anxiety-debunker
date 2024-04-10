package coden.anxiety.debunker.telegram.db

import coden.utils.success
import coden.utils.successOrElse
import org.telegram.abilitybots.api.db.MapDBContext

const val OWNER_ANXIETY_MESSAGES = "OWNER_ANXIETY_MESSAGES"
const val BOT_ANXIETY_MESSAGES = "BOT_ANXIETY_MESSAGES"

open class AnxietyDBContext(filename: String) : MapDBContext(db(filename)) {

    private val ownerMessages
        get() = getMap<OwnerMessage, String>(OWNER_ANXIETY_MESSAGES)

    private val botMessages
        get() = getSet<AnxietyLinkMessage>(BOT_ANXIETY_MESSAGES)

    fun addAnxietyToMessagesLink(anxietyId: String, inMessage: OwnerMessage, outMessage: BotMessage){
        addOwnerMessageLink(anxietyId, inMessage)
        addBotMessageLink(anxietyId, outMessage)
    }

    fun addOwnerMessageLink(anxietyId: String, ownerMessage: OwnerMessage) {
        ownerMessages[ownerMessage] = anxietyId
    }

    fun addBotMessageLink(anxietyId: String, ownerMessage: BotMessage) {
        botMessages.add(AnxietyLinkMessage(ownerMessage, anxietyId))
    }

    fun getOwnerMessageByAnxiety(anxietyId: String): Result<OwnerMessage> {
        return ownerMessages
            .filterValues { it == anxietyId }
            .maxByOrNull { it.key.id }
            .successOrElse(IllegalArgumentException("No owner message for this anxiety $anxietyId"))
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
            .successOrElse(IllegalArgumentException("Unable to find anxiety for $ownerMessage"))
    }

    fun getAnxietyByBotMessage(botMessage: BotMessage): Result<String> {
        return botMessages
            .firstOrNull { it.message == botMessage }
            ?.anxietyId
            .successOrElse(IllegalArgumentException("Unable to find anxiety for $botMessage"))
    }
}
