package coden.anxiety.debunker.telebot

import coden.anxiety.debunker.core.api.*
import org.apache.logging.log4j.kotlin.Logging
import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.objects.*
import org.telegram.abilitybots.api.util.AbilityUtils.getChatId
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup


class AnxietyDebunkerTelegramBot(
    private val config: TelegramBotConfig,
    private val analyser: AnxietyAnalyser,
    private val holder: AnxietyHolder,
    private val resolver: AnxietyResolver,
    private val formatter: AnxietyFormatter
) : AbilityBot(config.token, config.username),StartableLongPollingBot, Logging {
    override fun creatorId(): Long {
        return config.target
    }

    private val anxietyToBotMessage: MutableMap<String, Int> = HashMap()
    private val botMessageToAnxiety: MutableMap<Int, String> = HashMap()
    private val ownerMessageToAnxiety: MutableMap<Int, String> = HashMap()

    override fun start() {
        silent.sendMd(config.intro, config.target)
    }

    fun startAbility(): Ability {
        return Ability
            .builder()
            .name("start")
            .input(0)
            .action { silent.sendMd(config.intro, config.target)}
            .locality(Locality.USER)
            .privacy(Privacy.ADMIN)
            .build()
    }

    fun anxietyStats(): Ability{
        return Ability
            .builder()
            .name("stat")
            .input(0)
            .action { displayStats(it.update()) }
            .locality(Locality.USER)
            .privacy(Privacy.ADMIN)
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

    fun displayStats(update: Update) {
        val anxieties = analyser
            .anxieties(ListAnxietiesRequest(AnxietyFilter.ALL))
            .onFailure { silent.send(it.message, getChatId(update)) }
            .getOrNull() ?: return

        try {
            val s = "<pre>${formatter.format(anxieties)}</pre>"
            val message = SendMessage().apply {
                text = s
                enableHtml(true)
                chatId = getChatId(update).toString()
            }
            sender.execute(message)
        }catch (e:Exception){
            silent.send("Error: ${e.message}", getChatId(update))
        }

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

    fun resolve(): Reply{
        return Reply.of({b, u -> handleCallback(u, u.callbackQuery.data)}, Flag.CALLBACK_QUERY)
    }

    private fun handleCallback(update: Update, data: String){
        logger.info("Handling callback for $data")
        when(data){
            FULFILL.data -> resolve(update, true)
            UNFULFILL.data -> resolve(update, false)
            UNRESOLVE.data -> unresolve(update)
        }
    }

    private fun unresolve(update: Update){
        logger.info("Unresolved callback")
        val target = update.callbackQuery.message.messageId
        val anxiety = botMessageToAnxiety[target]
        if (anxiety == null) {
            silent.send("Could not find corresponding anxiety for resolution", getChatId(update))
            return
        }
        val result = resolver.unresolve(UnresolveAnxietyRequest(anxiety))
            .onFailure { silent.send(it.message, getChatId(update)) }
            .getOrNull() ?: return
        updateReplyMarkup(target, result.anxietyId, getChatId(update))
        updateDisplay(target, result.anxietyId, getChatId(update))
    }


    private fun resolve(update: Update, fulfilled: Boolean){
        logger.info("Resolved callback")
        val target = update.callbackQuery.message.messageId
        val anxiety = botMessageToAnxiety[target]
        if (anxiety == null) {
            silent.send("Could not find corresponding anxiety for resolution", getChatId(update))
            return
        }
        val result = resolver.resolve(ResolveAnxietyRequest(anxiety, fulfilled))
            .onFailure { silent.send(it.message, getChatId(update)) }
            .getOrNull() ?: return
        updateReplyMarkup(target, result.anxietyId, getChatId(update))
        updateDisplay(target, result.anxietyId, getChatId(update))
    }


    private fun updateAnxiety(update: Update) {
        val anxiety = ownerMessageToAnxiety[update.editedMessage.messageId]
        if (anxiety == null) {
            silent.send("Unable to find corresponding anxiety", getChatId(update))
            return
        }

        val botMessage: Int? = anxietyToBotMessage[anxiety]
        if (botMessage == null) {
            silent.send("Unable to find anxiety message", getChatId(update))
            return
        }
        val updated = holder.update(UpdateAnxietyRequest(anxiety, update.editedMessage.text))
            .onFailure { silent.send(it.message, getChatId(update)) }
            .getOrNull() ?: return

        updateDisplay(botMessage, updated.id, getChatId(update))
    }

    private fun updateReplyMarkup(
        target: Int,
        id: String,
        chatId: Long
    ) {
        try {
            val anxietyEntity = analyser.anxiety(AnxietyRequest(id))
                .onFailure { silent.send("Unable to get anxiety entity: ${it.message}", chatId) }
                .getOrNull() ?: return

            val markup = when(anxietyEntity.resolution){
                AnxietyEntityResolution.UNRESOLVED -> withNewAnxietyButtons()
                else -> withResolvedAnxietyButtons()
            }
            val edit = EditMessageReplyMarkup()
            edit.replyMarkup = markup
            edit.messageId = target

            edit.chatId = chatId.toString()
            sender.execute(edit)
        } catch (e: Exception) {
            silent.send("Anxiety could not be updated: ${e.message}", chatId)
        }
    }

    private fun updateDisplay(
        target: Int,
        id: String,
        chatId: Long
    ) {
        try {
            val anxietyEntity = analyser.anxiety(AnxietyRequest(id))
                .onFailure { silent.send("Unable to get anxiety entity: ${it.message}", chatId) }
                .getOrNull() ?: return

            val markup = when(anxietyEntity.resolution){
                AnxietyEntityResolution.UNRESOLVED -> withNewAnxietyButtons()
                else -> withResolvedAnxietyButtons()
            }
            val edit = EditMessageText()
            edit.text = formatter.formatAnxiety(
                anxietyEntity.id,
                anxietyEntity.created,
                anxietyEntity.description,
                anxietyEntity.resolution
            )
            edit.replyMarkup = markup
            edit.messageId = target
            edit.enableMarkdown(true)
            edit.chatId = chatId.toString()
            sender.execute(edit)
        } catch (e: Exception) {
            silent.send("Anxiety could not be updated: ${e.message}", chatId)
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
                text = formatter.formatAnxiety(newAnxiety.id, newAnxiety.created, newAnxiety.description, AnxietyEntityResolution.UNRESOLVED)
                chatId = getChatId(u).toString()
                enableMarkdown(true)
                replyMarkup = withNewAnxietyButtons()
            }
            val owner = u.message.messageId
            val bot = sender.execute(message).messageId
            anxietyToBotMessage[newAnxiety.id] = bot
            botMessageToAnxiety[bot] = newAnxiety.id
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


    fun withNewAnxietyButtons(): InlineKeyboardMarkup {
        return keyboard {
            row { b(FULFILL); b(UNFULFILL) }
        }
    }

    fun withResolvedAnxietyButtons(): InlineKeyboardMarkup {
        return keyboard {
            row { b(UNRESOLVE) }
        }
    }

    private val UNRESOLVE = KeyboardButton("↩\uFE0F Unresolve", "UNRESOLVE")
    private val FULFILL = KeyboardButton("❌ Fucked", "FULLFILL")
    private val UNFULFILL = KeyboardButton("✅ Fine", "UNFULFILLED")
}