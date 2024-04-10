package coden.anxiety.debunker.telegram.bot

import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.objects.*
import org.telegram.abilitybots.api.sender.MessageSender
import org.telegram.abilitybots.api.util.AbilityUtils.getChatId
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import java.util.function.Predicate

fun replyOn(filter: (Update) -> Boolean, handle: (Update) -> Unit): Reply {
    return Reply.of({bot, u ->
        tryHandle(handle, u, bot)
    }, filter)
}

fun replyOn(filter: Predicate<Update>, handle: (Update) -> Unit): Reply {
    return replyOn({filter.test(it)}, handle)
}

fun tryHandle(
    handle: (Update) -> Unit,
    u: Update,
    bot: BaseAbilityBot
) {
    try {
        handle(u)
    } catch (e: Exception) {
        bot.silent().send("⚠ ${e.message}", u.chatId())
    }
}

fun ability(cmd: String, handle: (Update) -> Unit): Ability{
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

fun MessageSender.sendHtml(text: String, chatId: Long, replyMarkup: ReplyKeyboard?=null): Message {
    val message = SendMessage().apply {
        enableHtml(true)
        this.text = text
        this.chatId = chatId.toString()
        this.replyMarkup = replyMarkup
    }
    return execute(message)
}

fun MessageSender.sendMd(text: String, chatId: Long, replyMarkup: ReplyKeyboard?=null): Message {
    val message = SendMessage().apply {
        enableMarkdown(true)
        this.text = text
        this.chatId = chatId.toString()
        this.replyMarkup = replyMarkup
    }
    return execute(message)
}

fun MessageSender.editMd(messageId: Int, text: String, chatId: Long, replyMarkup: InlineKeyboardMarkup?=null) {
    val message = EditMessageText().apply {
        enableMarkdown(true)
        this.messageId = messageId
        this.text = text
        this.chatId = chatId.toString()
        this.replyMarkup = replyMarkup
    }
    execute(message)
}

fun String.asCodeSnippet() = "<pre>$this</pre>"

fun justText(update: Update): Boolean {
    return Flag.TEXT.test(update) && !update.message.text.startsWith("/")
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