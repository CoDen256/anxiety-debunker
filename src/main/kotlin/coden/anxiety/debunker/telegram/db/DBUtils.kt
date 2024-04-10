package coden.anxiety.debunker.telegram.db

import org.mapdb.DB
import org.mapdb.DBMaker
import org.telegram.telegrambots.meta.api.objects.MaybeInaccessibleMessage
import java.io.Serializable


fun db(name: String): DB {
    return DBMaker.fileDB(name)
        .fileMmapEnableIfSupported()
        .closeOnJvmShutdown()
        .transactionEnable()
        .make()

}

data class AnxietyLinkMessage(
    val message: BotMessage,
    val anxietyId: String
): Serializable

data class OwnerMessage(val id: Int): Serializable {
    companion object {
        fun MaybeInaccessibleMessage.asOwner(): OwnerMessage {
            return OwnerMessage(this.messageId)
        }
    }

}

data class BotMessage(val id: Int): Serializable {
    companion object {
        fun MaybeInaccessibleMessage.asBot(): BotMessage {
            return BotMessage(this.messageId)
        }
    }
}
