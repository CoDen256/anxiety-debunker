package coden.anxiety.debunker

import coden.anxiety.debunker.core.impl.DefaultAnxietyAnalyser
import coden.anxiety.debunker.core.impl.DefaultAnxietyHolder
import coden.anxiety.debunker.core.impl.DefaultAnxietyResolver
import coden.anxiety.debunker.core.persistance.AnxietyRepository
import coden.anxiety.debunker.inmemory.InMemoryAnxietyRepository
import coden.anxiety.debunker.telebot.AnxietyDebunkerTelegramBot
import coden.anxiety.debunker.telebot.TelegramBotConfig
import coden.anxiety.debunker.telebot.TelegramBotConsole
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource

data class Config(
    val telegram: TelegramBotConfig
)

fun config(): Config{
    return ConfigLoaderBuilder.default()
        .addFileSource("application.yml")
        .build()
        .loadConfigOrThrow<Config>()
}

fun main() {
    val config = config()

    val repository: AnxietyRepository = InMemoryAnxietyRepository()

    val resolver = DefaultAnxietyResolver(repository)
    val holder = DefaultAnxietyHolder(repository)
    val analyser = DefaultAnxietyAnalyser(repository)

    val bot = AnxietyDebunkerTelegramBot(config.telegram,
        analyser,
        holder,
        resolver
    )
    val console = TelegramBotConsole(bot)

    console.start()
}