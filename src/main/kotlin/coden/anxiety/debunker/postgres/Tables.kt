package coden.anxiety.debunker.postgres

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

object Resolutions: Table("resolutions") {
    val anxietyId: Column<String> = reference("anxiety_id", Anxieties.id).uniqueIndex()
    val resolved: Column<Instant> = timestamp("resolved")
    val fulfilled: Column<Boolean> = bool("fulfilled")
}

object RiskAssessments: Table("risk_assessments") {
    val id: Column<String> = varchar("id", 5).uniqueIndex()
    val anxietyId: Column<String> = reference("anxiety_id", Anxieties.id).uniqueIndex()
    val assessed: Column<Instant> = timestamp("assessed")
    val risk: Column<Int> = integer("risk")
}