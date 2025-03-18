package io.github.coden.anxiety.debunker.postgres

import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Anxieties: Table("anxieties") {

    val id: Column<String> = varchar("id", 5).uniqueIndex()
    val description: Column<String> = varchar("description", 1024)
    val created: Column<Instant> = timestamp("created")
    override val primaryKey: PrimaryKey = PrimaryKey(id)
}

object AnxietyDetails: Table("anxiety_details") {
    val anxietyId: Column<String> = reference("anxiety_id", Anxieties.id).uniqueIndex()
    val trigger: Column<String> = varchar("trigger", 1024)
    val bodyResponse: Column<String> = varchar("body_response", 1024)
    val anxietyResponse: Column<String> = varchar("anxiety_response", 1024)
    val alternativeThoughts: Column<String> = varchar("alternative_thoughts", 1024)
}

object Resolutions: Table("resolutions") {
    val anxietyId: Column<String> = reference("anxiety_id", Anxieties.id).uniqueIndex()
    val created: Column<Instant> = timestamp("created")
    val fulfilled: Column<Boolean> = bool("fulfilled")
}

object ChanceAssessments: Table("chances") {
    val id: Column<String> = varchar("id", 5)
    val anxietyId: Column<String> = reference("anxiety_id", Anxieties.id)
    val created: Column<Instant> = timestamp("created")
    val chance: Column<Int> = integer("chance")
}