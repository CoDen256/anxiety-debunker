package coden.anxiety.debunker.telebot

import coden.anxiety.debunker.core.api.AnxietyAnalyser
import coden.anxiety.debunker.core.api.AnxietyHolder
import coden.anxiety.debunker.core.api.AnxietyResolver
import coden.anxiety.debunker.core.api.Console
import org.telegram.abilitybots.api.bot.AbilityBot

class AnxietyDebunkerTelegramBot(
    private val config: TelegramBotConfig,
    private val analyser: AnxietyAnalyser,
    private val holder: AnxietyHolder,
    private val resolver: AnxietyResolver
): AbilityBot(config.token, ), Console {
    override fun creatorId(): Long {
        TODO("Not yet implemented")
    }
}