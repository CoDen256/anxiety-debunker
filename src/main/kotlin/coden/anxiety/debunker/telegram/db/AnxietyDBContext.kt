package coden.anxiety.debunker.telegram.db

import org.telegram.abilitybots.api.db.MapDBContext

const val OWNER_ANXIETY_MESSAGES = "OWNER_ANXIETY_MESSAGES"
const val BOT_ANXIETY_MESSAGES = "BOT_ANXIETY_MESSAGES"

open class AnxietyDBContext(filename: String) : MapDBContext(db(filename)) {

    private val ownerMessages
        get() = getMap<OwnerMessage, String>(OWNER_ANXIETY_MESSAGES)

    private val botMessages
        get() = getSet<AnxietyBoundMessage>(BOT_ANXIETY_MESSAGES)

    fun addOwnerMessage(anxietyId: String, ownerMessage: OwnerMessage) {
        ownerMessages[ownerMessage] = anxietyId
    }

    fun addBotMessage(anxietyId: String, ownerMessage: BotMessage) {
        botMessages.add(AnxietyBoundMessage(ownerMessage, anxietyId))
    }

    fun getBotMessagesByAnxiety(anxietyId: String): Set<BotMessage> {
        return botMessages
            .filter { it.anxietyId == anxietyId }
            .map { it.message }
            .toSet()
    }

    fun getAnxietyByOwnerMessage(ownerMessage: OwnerMessage): String? {
        return ownerMessages[ownerMessage]
    }

    fun getAnxietyByBotMessage(botMessage: BotMessage): String? {
        return botMessages
            .firstOrNull { it.message == botMessage }
            ?.anxietyId
    }
}
