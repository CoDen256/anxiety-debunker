package coden.anxiety.debunker.telebot

import coden.anxiety.debunker.core.api.AnxietyAnalyser
import coden.anxiety.debunker.core.api.AnxietyHolder
import coden.anxiety.debunker.core.api.AnxietyResolver
import org.telegram.abilitybots.api.bot.AbilityBot
import org.telegram.abilitybots.api.objects.Ability
import org.telegram.abilitybots.api.objects.Privacy
import org.telegram.abilitybots.api.objects.Locality.USER
import org.telegram.abilitybots.api.util.AbilityUtils.getChatId
import org.telegram.telegrambots.meta.api.objects.Update

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
            .locality(USER)
            .privacy(Privacy.PUBLIC)
            .action { sendHelloWorld(it.update()) }
            .build()
    }
}