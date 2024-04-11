package io.github.coden.anxiety.debunker

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import io.github.coden.anxiety.debunker.core.impl.DefaultAnxietyAnalyser
import io.github.coden.anxiety.debunker.core.impl.DefaultAnxietyAssessor
import io.github.coden.anxiety.debunker.core.impl.DefaultAnxietyHolder
import io.github.coden.anxiety.debunker.core.impl.DefaultAnxietyResolver
import io.github.coden.anxiety.debunker.core.persistance.AnxietyRepository
import io.github.coden.anxiety.debunker.inmemory.InMemoryAnxietyRepository
import io.github.coden.anxiety.debunker.postgres.AnxietyDatabaseRepository
import io.github.coden.anxiety.debunker.telegram.bot.AnxietyRecorderTelegramBot
import io.github.coden.anxiety.debunker.telegram.bot.AnxietyDebunkerTelegramBot
import io.github.coden.anxiety.debunker.telegram.db.AnxietyBotDB
import io.github.coden.anxiety.debunker.telegram.formatter.AnxietyTelegramFormatter
import io.github.coden.database.DatasourceConfig
import io.github.coden.database.database
import io.github.coden.telegram.abilities.TelegramBotConfig
import io.github.coden.telegram.run.TelegramBotConsole

data class RepositoryConfig(
    val inmemory: Boolean = true,
    val datasource: DatasourceConfig?
)

data class Config(
    val debunker: TelegramBotConfig,
    val recorder: TelegramBotConfig,
    val repo: RepositoryConfig
)

fun config(): Config{
    return ConfigLoaderBuilder.default()
        .addFileSource("application.yml")
        .build()
        .loadConfigOrThrow<Config>()
}

fun repo(repo: RepositoryConfig): AnxietyRepository {
    if (repo.inmemory) return InMemoryAnxietyRepository()
    return AnxietyDatabaseRepository(database(repo.datasource!!))
}

fun main() {
    val config = config()
    val repository: AnxietyRepository = repo(config.repo)

    val resolver = DefaultAnxietyResolver(repository)
    val holder = DefaultAnxietyHolder(repository)
    val analyser = DefaultAnxietyAnalyser(repository)
    val assessor = DefaultAnxietyAssessor(repository)
    val formatter = AnxietyTelegramFormatter()
    val debunkerDb = AnxietyBotDB("debunker.db")
    val recorderDb = AnxietyBotDB("recorder.db")

    val debunker = AnxietyDebunkerTelegramBot(
        config.debunker,
        debunkerDb,
        analyser,
        holder,
        resolver,
        assessor,
        formatter,
    )

    val recorder = AnxietyRecorderTelegramBot(
        config.recorder,
        recorderDb,
        analyser,
        holder,
        resolver,
        assessor,
        formatter,
    )

    val console = TelegramBotConsole(
        debunker,
        recorder
    )

    console.start()
}