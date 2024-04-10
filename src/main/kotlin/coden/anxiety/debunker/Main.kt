package coden.anxiety.debunker

import coden.anxiety.debunker.core.impl.DefaultAnxietyAnalyser
import coden.anxiety.debunker.core.impl.DefaultAnxietyAssessor
import coden.anxiety.debunker.core.impl.DefaultAnxietyHolder
import coden.anxiety.debunker.core.impl.DefaultAnxietyResolver
import coden.anxiety.debunker.core.persistance.*
import coden.anxiety.debunker.postgres.AnxietyDatabaseRepository
import coden.anxiety.debunker.postgres.DatasourceConfig
import coden.anxiety.debunker.postgres.database
import coden.anxiety.debunker.telegram.bot.AnxietyDebunkerTelegramBot
import coden.anxiety.debunker.telegram.formatter.AnxietyTelegramFormatter
import coden.anxiety.debunker.telegram.TelegramBotConfig
import coden.anxiety.debunker.telegram.TelegramBotConsole
import coden.anxiety.debunker.telegram.db.AnxietyDebunkerDBContext
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource

data class Config(
    val telegram: TelegramBotConfig,
    val datasource: DatasourceConfig
)

fun config(): Config{
    return ConfigLoaderBuilder.default()
        .addFileSource("application.yml")
        .build()
        .loadConfigOrThrow<Config>()
}

fun main() {
    val config = config()
    val db = database(config.datasource)
    val repository: AnxietyRepository = AnxietyDatabaseRepository(db)
    repository.anxiety("abcde").getOrThrow()

    val resolver = DefaultAnxietyResolver(repository)
    val holder = DefaultAnxietyHolder(repository)
    val analyser = DefaultAnxietyAnalyser(repository)
    val assessor = DefaultAnxietyAssessor(repository)
    val formatter = AnxietyTelegramFormatter()
    val dbContext = AnxietyDebunkerDBContext("debunker.db")

    val bot = AnxietyDebunkerTelegramBot(
        config.telegram,
        analyser,
        holder,
        resolver,
        assessor,
        formatter,
        dbContext
    )
    val console = TelegramBotConsole(bot)

    console.start()
}