package coden.anxiety.debunker.telegram.bot

import coden.utils.successOrElse
import org.telegram.telegrambots.abilitybots.api.bot.BaseAbilityBot
import org.telegram.telegrambots.abilitybots.api.objects.*
import org.telegram.telegrambots.abilitybots.api.util.AbilityUtils.getChatId
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText.EditMessageTextBuilder
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.reactions.ReactionTypeEmoji
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.util.function.Predicate


fun replyOn(filter: (Update) -> Boolean, handle: (Update) -> Unit): Reply {
    return Reply.of({bot, u ->
        tryHandle(handle, u, bot)
    }, filter)
}

fun replyOn(filter: Predicate<Update>, handle: (Update) -> Unit): Reply {
    return replyOn({filter.test(it)}, handle)
}

fun replyOnReaction(vararg emojis: String, handle: (Update) -> Unit): Reply {
    return replyOn({
        upd ->
        !upd.messageReaction?.newReaction.isNullOrEmpty()
            && upd.messageReaction
                .newReaction
                .filterIsInstance<ReactionTypeEmoji>()
                .any { emojis.contains(it.emoji) }
                   }, handle)
}

fun tryHandle(
    handle: (Update) -> Unit,
    u: Update,
    bot: BaseAbilityBot
) {
    try {
        handle(u)
    } catch (e: Exception) {
        bot.silent.send("⚠ ${e.message}: \n$e", u.chatId())
    }
}

fun ability(cmd: String, handle: (Update) -> Unit): Ability {
    return Ability.builder()
        .name(cmd)
        .input(0)
        .action { tryHandle(handle, it.update(), it.bot()) }
        .locality(Locality.USER)
        .privacy(Privacy.ADMIN)
        .build()
}

fun replyOnCallback(handle: (Update, String) -> Unit): Reply = replyOn(Flag.CALLBACK_QUERY){
    handle(it, it.callbackQuery.data)
}

fun Update.chatId(): Long = getChatId(this)
fun Update.strChatId(): String = chatId().toString()

fun TelegramClient.sendHtml(text: String, chatId: Long, replyMarkup: ReplyKeyboard?=null): Message {
    val message = SendMessage.builder().apply {
        parseMode("html")
        text(text)
        chatId(chatId.toString())
        replyMarkup(replyMarkup)
    }.build()
    return execute(message)
}

fun TelegramClient.sendMd(text: String,
                         chatId: Long,
                         replyMarkup: ReplyKeyboard?=null,
                         replyTo: Int?=null): Message {
    val message = SendMessage.builder().apply {
        parseMode("Markdown")
        text( text)
        chatId( chatId.toString())
        replyToMessageId( replyTo)
        replyMarkup( replyMarkup)
    }.build()
    return execute(message)
}

fun TelegramClient.editMdRequest(text: String,
                                chatId: Long,
                                replyMarkup: InlineKeyboardMarkup?=null,
                                messageId: Int?=null): EditMessageTextBuilder<*, *> {
    return EditMessageText.builder().apply {
        parseMode("Markdown")
        messageId(messageId)
        text(text)
        chatId(chatId.toString())
        replyMarkup(replyMarkup)

    }
}

fun TelegramClient.editMd(messageId: Int, text: String, chatId: Long, replyMarkup: InlineKeyboardMarkup?=null) {
    val request = editMdRequest(text=text, chatId, replyMarkup, messageId ).build()
    execute(request)
}

fun String.asCodeSnippet() = "<pre>$this</pre>"

fun justText(update: Update): Boolean {
    return Flag.TEXT.test(update) && !isCommand(update) && !isId(update)
}

fun isCommand(update: Update) = update.message.text.startsWith("/")
fun isId(update: Update) = update.message.text.startsWith("#")

fun getId(update: Update): Result<String>{
    if (!isId(update)) return Result.failure(IllegalArgumentException("${update.message.text} does not represent an id"))

    return update.message.text
        .split(" ")
        .firstOrNull()
        ?.drop(1)
        .successOrElse(IllegalArgumentException("Cannot parse ${update.message.text} as id"))
}

fun cleanText(u: Update): String {
    return cleanText(u.message)
}

fun cleanText(message: Message): String {
    if (message.text.startsWith("/")){
        if (!message.text.contains(" ")) return ""
        return message.text.split(" ", limit = 2)[1]
    }
    return message.text
}