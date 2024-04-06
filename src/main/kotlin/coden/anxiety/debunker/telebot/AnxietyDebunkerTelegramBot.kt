package coden.anxiety.debunker.telebot

import coden.anxiety.debunker.core.api.*
import coden.anxiety.debunker.core.persistance.Anxiety
import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.objects.*
import org.telegram.abilitybots.api.util.AbilityUtils.getChatId
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AnxietyDebunkerTelegramBot(
    private val config: TelegramBotConfig,
    private val analyser: AnxietyAnalyser,
    private val holder: AnxietyHolder,
    private val resolver: AnxietyResolver
) : AbilityBot(config.token, config.username) {
    override fun creatorId(): Long {
        return config.target
    }

    private val anxietyToBotMessage: MutableMap<String, Int> = HashMap()
    private val ownerMessageToAnxiety: MutableMap<Int, String> = HashMap()

    private fun sendHelloWorld(update: Update) {
        silent.send("Hello world", getChatId(update))
    }

    fun sayHelloWorldOnStart(): Ability {
        return Ability
            .builder()
            .name("start")
            .locality(Locality.USER)
            .privacy(Privacy.PUBLIC)
            .action { sendHelloWorld(it.update()) }
            .build()
    }

    fun anxiety(): Ability {
        return Ability
            .builder()
            .name("anxiety")
            .input(0)
            .action { handleAnxiety(it.update()) }
            .locality(Locality.USER)
            .privacy(Privacy.ADMIN)
            .build()
    }

    fun onAnxiety(): Reply {
        return Reply.of({ b, u -> handleAnxiety(u) }, { isNotCommand(it) })
    }

    fun editAnxiety(): Reply {
        return Reply.of({b, u -> updateAnxiety(u)}, Flag.EDITED_MESSAGE)
    }

    private fun isNotCommand(update: Update): Boolean {
        return Flag.TEXT.test(update) && !update.message.text.startsWith("/")
    }

    private fun updateAnxiety(update: Update) {
        val anxiety = ownerMessageToAnxiety[update.editedMessage.messageId]
        if (anxiety == null) {
            silent.send("Unable to find corresponding anxiety", getChatId(update))
            return
        }

        val botMessage = anxietyToBotMessage[anxiety]
        if (botMessage == null) {
            silent.send("Unable to find anxiety message", getChatId(update))
        }

        try {
            val edit = EditMessageText()
            val updated = holder.update(UpdateAnxietyRequest(anxiety, update.editedMessage.text))
                .onFailure { silent.send(it.message, getChatId(update)) }
                .getOrNull() ?: return

            val anxietyEntity = analyser.anxiety(AnxietyRequest(updated.id))
                .onFailure { silent.send("Unable to get anxiety entity: ${it.message}", getChatId(update)) }
                .getOrNull() ?: return
            edit.text = formatAnxiety(anxietyEntity.id, anxietyEntity.created, anxietyEntity.description, anxietyEntity.resolution)
            edit.messageId = botMessage
            edit.enableMarkdown(true)
            edit.chatId = getChatId(update).toString()
            sender.execute(edit)
        }catch (e:Exception){
            silent.send("Anxiety could not be updated: ${e.message}", getChatId(update))
        }
    }

    private fun handleAnxiety(u: Update) {
        silent.send("Gotcha", getChatId(u))

        val description = clean(u)

        val newAnxiety = holder.add(NewAnxietyRequest(description))
            .onFailure { silent.send("Unable to add new anxiety: ${it.message}", getChatId(u)) }
            .getOrNull() ?: return

        try {

            val message = SendMessage().apply {
                text = formatAnxiety(newAnxiety.id, newAnxiety.created, newAnxiety.description, AnxietyEntityResolution.UNRESOLVED)
                chatId = getChatId(u).toString()
                enableMarkdown(true)
                replyMarkup = withNewAnxietyButtons()
            }
            val owner = u.message.messageId
            val bot = sender.execute(message).messageId
            anxietyToBotMessage[newAnxiety.id] = bot
            ownerMessageToAnxiety[owner] = newAnxiety.id
        } catch (e: Exception) {
            silent.send("Error $e", getChatId(u))
        }

    }

    private fun clean(u: Update): String {
        if (u.message.text.startsWith("/")){
            if (!u.message.text.contains(" ")) return ""
            return u.message.text.split(" ", limit = 2)[1]
        }
        return u.message.text
    }

    private val formatter = DateTimeFormatter.ofPattern("d MMM HH:mm")

    fun formatAnxiety(id: String, created: Instant, description: String, resolution: AnxietyEntityResolution): String {
        return "*Anxiety* #${id} \\[`$resolution`]" +
                "\n${formatter.format(created.atZone(ZoneId.of("CET")))}" +
                "\n\n$description"
    }

    fun withNewAnxietyButtons(): ReplyKeyboard {
        return keyboard {
            row { b(FULFILL); b(UNFULFILL) }
        }
    }

    fun withResolvedAnxietyButtons(): ReplyKeyboard {
        return keyboard {
            row { b(UNRESOLVE) }
        }
    }

    private val UNRESOLVE = KeyboardButton("Unresolve")
    private val FULFILL = KeyboardButton("Fulfilled")
    private val UNFULFILL = KeyboardButton("Unfulfilled")
}