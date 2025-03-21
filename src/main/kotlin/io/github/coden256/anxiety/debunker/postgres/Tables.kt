package io.github.coden256.anxiety.debunker.postgres

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import io.github.coden256.anxiety.debunker.core.persistance.*
import io.github.coden256.anxiety.debunker.core.persistance.Chance.Companion.chance
import io.github.coden256.database.transaction
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Anxieties : Table("anxieties") {

    val id: Column<String> = varchar("id", 5).uniqueIndex()
    val description: Column<String> = varchar("description", 4096)
    val created: Column<Instant> = timestamp("created")
    override val primaryKey: PrimaryKey = PrimaryKey(id)
}

object AnxietyDetails : Table("anxiety_details") {
    val anxietyId: Column<String> = reference("anxiety_id", Anxieties.id).uniqueIndex()
    val trigger: Column<String> = varchar("trigger", 512)
    val bodyResponse: Column<String> = varchar("body_response", 1024)
    val anxietyResponse: Column<String> = varchar("anxiety_response", 1024)
    val alternativeThoughts: Column<String> = varchar("alternative_thoughts", 4096)
}

object Resolutions : Table("resolutions") {
    val anxietyId: Column<String> = reference("anxiety_id", Anxieties.id).uniqueIndex()
    val created: Column<Instant> = timestamp("created")
    val fulfilled: Column<Boolean> = bool("fulfilled")
}

object ChanceAssessments : Table("chances") {
    val id: Column<String> = varchar("id", 5)
    val anxietyId: Column<String> = reference("anxiety_id", Anxieties.id)
    val created: Column<Instant> = timestamp("created")
    val chance: Column<Int> = integer("chance")
}

val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]")

fun parseInstant(string: String?): java.time.Instant? {
    string ?: return null
    return try {
        LocalDateTime.parse(string, FORMATTER).atZone(ZoneId.of("UTC")).toInstant()
    } catch (e: Exception) {
        return null
    }
}

fun importAnxieties(path: String): List<Anxiety> {
    return csvReader()
        .readAllWithHeader(File(path))
        .map {
            Anxiety(
                description = it["description"] ?: return@map null,
                id = it["id"] ?: return@map null,
                created = parseInstant(it["created"]) ?: return@map null,
            )
        }
        .mapNotNull { it }
}

fun importAnxietyDetails(path: String): List<AnxietyDetail> {
    return csvReader()
        .readAllWithHeader(File(path))
        .mapNotNull {
            AnxietyDetail(
                anxietyId = it["anxiety_id"] ?: return@mapNotNull null,
                trigger = it["trigger"] ?: return@mapNotNull null,
                bodyResponse = it["body_response"] ?: return@mapNotNull null,
                anxietyResponse = it["anxiety_response"] ?: return@mapNotNull null,
                alternativeThoughts = it["alternative_thoughts"] ?: return@mapNotNull null,
            )
        }
}

fun importAnxietyResolutions(path: String): List<Resolution> {
    return csvReader()
        .readAllWithHeader(File(path))
        .mapNotNull {
            Resolution(
                anxietyId = it["anxiety_id"] ?: return@mapNotNull null,
                fulfilled = it["fullfilled"].toBoolean(),
                created = parseInstant(it["created"]) ?: return@mapNotNull null,
            )
        }
}

fun importAnxietyChances(path: String): List<ChanceAssessment> {
    return csvReader()
        .readAllWithHeader(File(path))
        .mapNotNull {
            ChanceAssessment(
                anxietyId = it["anxiety_id"] ?: return@mapNotNull null,
                id = it["id"] ?: return@mapNotNull null,
                chance = it["chance"]?.toInt()?.chance() ?: return@mapNotNull null,
                created = parseInstant(it["created"]) ?: return@mapNotNull null,
            )
        }
}

fun migrate(repository: AnxietyRepository){
    val anxieties = importAnxieties("anxieties.csv")
    val details = importAnxietyDetails("anxiety_details.csv")
    val resolutions = importAnxietyResolutions("resolutions.csv")
    val chances = importAnxietyChances("chances.csv")

    anxieties.forEach { repository.saveAnxiety(it) }.also { println("Saved ${anxieties.size} anxiety") }
    details.forEach { repository.saveDetail(it) }.also { println("Saved ${anxieties.size} details") }
    resolutions.forEach { repository.saveResolution(it) }.also { println("Saved ${anxieties.size} resolutions") }
    chances.forEach { repository.saveChanceAssessment(it) }.also { println("Saved ${anxieties.size} chances") }
}

fun Database.createTables() {
    transaction {
        SchemaUtils.create(AnxietyDetails, Anxieties, Resolutions, ChanceAssessments)
    }
}