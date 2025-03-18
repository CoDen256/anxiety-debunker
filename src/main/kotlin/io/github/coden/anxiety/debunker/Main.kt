package io.github.coden.anxiety.debunker

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import io.github.coden.anxiety.debunker.core.impl.*
import io.github.coden.anxiety.debunker.core.persistance.AnxietyRepository
import io.github.coden.anxiety.debunker.inmemory.InMemoryAnxietyRepository
import io.github.coden.anxiety.debunker.postgres.*
import io.github.coden.anxiety.debunker.telegram.bot.AnxietyDebunkerTelegramBot
import io.github.coden.anxiety.debunker.telegram.db.AnxietyBotDB
import io.github.coden.anxiety.debunker.telegram.formatter.AnxietyTelegramFormatter
import io.github.coden.database.DatasourceConfig
import io.github.coden.database.database
import io.github.coden.database.transaction
import io.github.coden.telegram.abilities.TelegramBotConfig
import io.github.coden.telegram.run.TelegramBotConsole
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager

data class Config(
    val debunker: TelegramBotConfig,
    val repo: RepositoryConfig
)

data class RepositoryConfig(
    val inmemory: Boolean = true,
    val datasource: DatasourceConfig?
)

fun config(): Config{
    return ConfigLoaderBuilder.default()
        .addFileSource("application.yml")
        .build()
        .loadConfigOrThrow<Config>()
}

fun repo(repo: RepositoryConfig): AnxietyRepository {
    if (repo.inmemory) return InMemoryAnxietyRepository()
    val db = database(repo.datasource!!)
        return AnxietyDatabaseRepository(db)
}

fun Database.createTables() {
    transaction {
        TransactionManager.current().connection.prepareStatement("SET autocommit_before_ddl = on;", false).executeUpdate()
        SchemaUtils.create(AnxietyDetails, Anxieties, Resolutions, ChanceAssessments) }
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