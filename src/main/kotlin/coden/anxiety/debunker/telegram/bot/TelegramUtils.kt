package coden.anxiety.debunker.telegram.bot

import org.telegram.abilitybots.api.bot.BaseAbilityBot
import org.telegram.abilitybots.api.objects.*
import org.telegram.abilitybots.api.util.AbilityUtils.getChatId
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update

fun replyOn(filter: (Update) -> Boolean, handle: (Update) -> Unit): Reply {
    return Reply.of({bot, u ->
        tryHandle(handle, u, bot)
    }, filter)
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

fun Update.chatId(): Long = getChatId(this)
fun Update.strChatId(): String = chatId().toString()

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