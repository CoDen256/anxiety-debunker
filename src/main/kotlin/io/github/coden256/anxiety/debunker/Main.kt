package io.github.coden256.anxiety.debunker

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import com.sksamuel.hoplite.addResourceSource
import io.github.coden256.anxiety.debunker.core.impl.*
import io.github.coden256.anxiety.debunker.core.persistance.AnxietyRepository
import io.github.coden256.anxiety.debunker.inmemory.InMemoryAnxietyRepository
import io.github.coden256.anxiety.debunker.postgres.AnxietyDatabaseRepository
import io.github.coden256.anxiety.debunker.postgres.createTables
import io.github.coden256.anxiety.debunker.telegram.bot.AnxietyDebunkerTelegramBot
import io.github.coden256.anxiety.debunker.telegram.db.AnxietyBotDB
import io.github.coden256.anxiety.debunker.telegram.formatter.AnxietyTelegramFormatter
import io.github.coden256.database.DatasourceConfig
import io.github.coden256.database.database
import io.github.coden256.telegram.abilities.TelegramBotConfig
import io.github.coden256.telegram.run.TelegramBotConsole

data class Config(
    val debunker: TelegramBotConfig,
    val repo: RepositoryConfig
)

data class RepositoryConfig(
    val inmemory: Boolean = true,
    val datasource: DatasourceConfig?
)

fun config(): Config {
    return ConfigLoaderBuilder.default()
        .addResourceSource("/application.yml", optional = true)
        .addFileSource("application.yml", optional = true)
        .build()
        .loadConfigOrThrow<Config>()
}

fun repo(repo: RepositoryConfig): AnxietyRepository {
    if (repo.inmemory) return InMemoryAnxietyRepository()
    val db = database(repo.datasource!!)
    db.createTables()
    return AnxietyDatabaseRepository(db)
}

fun main() {
    val config = config()
    val repository: AnxietyRepository = repo(config.repo)

    val resolver = DefaultAnxietyResolver(repository)
    val holder = DefaultAnxietyHolder(repository)
    val analyser = DefaultAnxietyAnalyser(repository)
    val assessor = DefaultAnxietyAssessor(repository)
    val editor = DefaultAnxietyDetailEditor(repository)
    val formatter = AnxietyTelegramFormatter()
    val debunkerDb = AnxietyBotDB("debunker.db")

    val debunker = AnxietyDebunkerTelegramBot(
        config.debunker,
        debunkerDb,
        analyser,
        holder,
        resolver,
        assessor,
        editor,
        formatter,
    )

    val console = TelegramBotConsole(
        debunker
    )

    console.start()
}