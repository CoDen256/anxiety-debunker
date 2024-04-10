package coden.anxiety.debunker.postgres

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction

object AnxietyTable: Table("anxieties") {

    val id: Column<String> = varchar("id", 5)
    val description: Column<String> = varchar("description", 1024)
    val created: Column<Instant> = timestamp("created")
    override val primaryKey: PrimaryKey = PrimaryKey(id)
}


fun main() {
    transaction {
        val anxietyTable = AnxietyTable.insert {
            it[id] = "abcde"
            it[created] = Clock.System.now()
            it[description] = "Hello this is the first one"
        }
    }
}