package coden.anxiety.debunker.telebot

import coden.anxiety.debunker.core.api.AnxietyAnalyser
import coden.anxiety.debunker.core.api.AnxietyHolder
import coden.anxiety.debunker.core.api.AnxietyResolver
import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.objects.*
import org.telegram.abilitybots.api.util.AbilityUtils.getChatId
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard

class AnxietyDebunkerTelegramBot(
    private val config: TelegramBotConfig,
    private val analyser: AnxietyAnalyser,
    private val holder: AnxietyHolder,
    private val resolver: AnxietyResolver
) : AbilityBot(config.token, config.username) {
    override fun creatorId(): Long {
        return config.target
    }

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

    private fun isNotCommand(update: Update): Boolean {
        return Flag.TEXT.test(update) && !update.message.text.startsWith("/")
    }

    private fun handleAnxiety(u: Update) {
        silent.send("Gotcha", getChatId(u))
    }

    fun withNewAnxietyButtons(): ReplyKeyboard {
        return null!!
    }
}