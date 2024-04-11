package coden.anxiety.debunker

import coden.anxiety.debunker.core.impl.DefaultAnxietyAnalyser
import coden.anxiety.debunker.core.impl.DefaultAnxietyAssessor
import coden.anxiety.debunker.core.impl.DefaultAnxietyHolder
import coden.anxiety.debunker.core.impl.DefaultAnxietyResolver
import coden.anxiety.debunker.core.persistance.AnxietyRepository
import coden.anxiety.debunker.inmemory.InMemoryAnxietyRepository
import coden.anxiety.debunker.postgres.AnxietyDatabaseRepository
import coden.anxiety.debunker.postgres.DatasourceConfig
import coden.anxiety.debunker.postgres.database
import coden.anxiety.debunker.telegram.TelegramBotConfig
import coden.anxiety.debunker.telegram.TelegramBotConsole
import coden.anxiety.debunker.telegram.bot.AnxietyDebunkerTelegramBot
import coden.anxiety.debunker.telegram.bot.AnxietyRecorderTelegramBot
import coden.anxiety.debunker.telegram.db.AnxietyDBContext
import coden.anxiety.debunker.telegram.formatter.AnxietyTelegramFormatter
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource

data class Config(
    val debunker: TelegramBotConfig,
    val recorder: TelegramBotConfig,
    val datasource: DatasourceConfig
)

fun config(): Config{
    return ConfigLoaderBuilder.default()
        .addFileSource("application.yml")
        .build()
        .loadConfigOrThrow<Config>()
}

fun repo(datasource: DatasourceConfig): AnxietyRepository {
    if (datasource.inmemory) return InMemoryAnxietyRepository()
    return AnxietyDatabaseRepository(database(datasource))
}

fun main() {
    val config = config()
    val repository: AnxietyRepository = repo(config.datasource)

    val resolver = DefaultAnxietyResolver(repository)
    val holder = DefaultAnxietyHolder(repository)
    val analyser = DefaultAnxietyAnalyser(repository)
    val assessor = DefaultAnxietyAssessor(repository)
    val formatter = AnxietyTelegramFormatter()
    val debunkerDb = AnxietyDBContext("debunker.db")
    val recorderDb = AnxietyDBContext("recorder.db")

    val debunker = AnxietyDebunkerTelegramBot(
        config.debunker,
        analyser,
        holder,
        resolver,
        assessor,
        formatter,
        debunkerDb,
    )

    val recorder = AnxietyRecorderTelegramBot(
        config.recorder,
        analyser,
        holder,
        resolver,
        assessor,
        formatter,
        recorderDb,
    )

    val console = TelegramBotConsole(
        debunker,
        recorder
    )

    console.start()
}