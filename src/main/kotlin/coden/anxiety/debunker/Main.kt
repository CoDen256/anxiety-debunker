package coden.anxiety.debunker

import coden.anxiety.debunker.core.impl.DefaultAnxietyAnalyser
import coden.anxiety.debunker.core.impl.DefaultAnxietyAssessor
import coden.anxiety.debunker.core.impl.DefaultAnxietyHolder
import coden.anxiety.debunker.core.impl.DefaultAnxietyResolver
import coden.anxiety.debunker.core.persistance.*
import coden.anxiety.debunker.inmemory.InMemoryAnxietyRepository
import coden.anxiety.debunker.telegram.bot.AnxietyDebunkerTelegramBot
import coden.anxiety.debunker.telegram.formatter.AnxietyTelegramFormatter
import coden.anxiety.debunker.telegram.TelegramBotConfig
import coden.anxiety.debunker.telegram.TelegramBotConsole
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
    val a = NewAnxietyEntity("The plane is gonna crash")
    repository.saveAnxiety(a)
    val b = NewAnxietyEntity("I have cancer")
    repository.saveAnxiety(b)
    val c = NewAnxietyEntity("The car will hit me")
    repository.saveAnxiety(c)
    repository.saveAnxiety(NewAnxietyEntity("I will fall out of the window "))
    repository.saveResolution(Resolution(a.id, true))
    repository.saveResolution(Resolution(b.id, false))
    repository.saveRiskAssessment(RiskAssessment(a.id, RiskLevel.MAX))
    repository.saveRiskAssessment(RiskAssessment(b.id, RiskLevel.MAX))
    repository.saveRiskAssessment(RiskAssessment(c.id, RiskLevel.MAX))

    val resolver = DefaultAnxietyResolver(repository)
    val holder = DefaultAnxietyHolder(repository)
    val analyser = DefaultAnxietyAnalyser(repository)
    val assessor = DefaultAnxietyAssessor(repository)
    val formatter = AnxietyTelegramFormatter()

    val bot = AnxietyDebunkerTelegramBot(
        config.telegram,
        analyser,
        holder,
        resolver,
        assessor,
        formatter,
    )
    val console = TelegramBotConsole(bot)

    console.start()
}