package io.github.coden.anxiety.debunker

import io.github.coden.anxiety.debunker.core.impl.DefaultAnxietyAnalyser
import io.github.coden.anxiety.debunker.core.impl.DefaultAnxietyAssessor
import io.github.coden.anxiety.debunker.core.impl.DefaultAnxietyHolder
import io.github.coden.anxiety.debunker.core.impl.DefaultAnxietyResolver
import io.github.coden.anxiety.debunker.core.persistance.AnxietyRepository
import io.github.coden.anxiety.debunker.inmemory.InMemoryAnxietyRepository
import io.github.coden.anxiety.debunker.postgres.AnxietyDatabaseRepository
import io.github.coden.anxiety.debunker.postgres.DatasourceConfig
import io.github.coden.anxiety.debunker.postgres.database
import io.github.coden.anxiety.debunker.telegram.TelegramBotConfig
import io.github.coden.anxiety.debunker.telegram.TelegramBotConsole
import io.github.coden.anxiety.debunker.telegram.bot.AnxietyDebunkerTelegramBot
import io.github.coden.anxiety.debunker.telegram.bot.AnxietyRecorderTelegramBot
import io.github.coden.anxiety.debunker.telegram.db.AnxietyDBContext
import io.github.coden.anxiety.debunker.telegram.formatter.AnxietyTelegramFormatter
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