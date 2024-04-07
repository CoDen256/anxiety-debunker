package coden.anxiety.debunker

import coden.anxiety.debunker.core.impl.DefaultAnxietyAnalyser
import coden.anxiety.debunker.core.impl.DefaultAnxietyHolder
import coden.anxiety.debunker.core.impl.DefaultAnxietyResolver
import coden.anxiety.debunker.core.persistance.Anxiety
import coden.anxiety.debunker.core.persistance.AnxietyRepository
import coden.anxiety.debunker.core.persistance.Resolution
import coden.anxiety.debunker.inmemory.InMemoryAnxietyRepository
import coden.anxiety.debunker.telebot.AnxietyDebunkerTelegramBot
import coden.anxiety.debunker.telebot.AnxietyTelegramFormatter
import coden.anxiety.debunker.telebot.TelegramBotConfig
import coden.anxiety.debunker.telebot.TelegramBotConsole
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import com.sksamuel.hoplite.yaml.YamlParser
import java.io.FileInputStream

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
    val a = Anxiety("The plane is gonna crash")
    repository.saveAnxiety(a)
    val b = Anxiety("I have cancer")
    repository.saveAnxiety(b)
    repository.saveAnxiety(Anxiety("The car will hit me"))
    repository.saveAnxiety(Anxiety("I will fall out of the window "))
    repository.saveResolution(Resolution(a.id, true))
    repository.saveResolution(Resolution(b.id, false))

    val resolver = DefaultAnxietyResolver(repository)
    val holder = DefaultAnxietyHolder(repository)
    val analyser = DefaultAnxietyAnalyser(repository)


    val bot = AnxietyDebunkerTelegramBot(
        config.telegram,
        analyser,
        holder,
        resolver,
        AnxietyTelegramFormatter()
    )
    val console = TelegramBotConsole(bot)

    console.start()
}