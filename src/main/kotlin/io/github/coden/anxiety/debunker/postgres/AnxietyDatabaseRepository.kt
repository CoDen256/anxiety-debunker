package io.github.coden.anxiety.debunker.postgres

import io.github.coden.anxiety.debunker.core.persistance.*
import io.github.coden.anxiety.debunker.core.persistance.Chance.Companion.chance
import io.github.coden.database.asDBInstant
import io.github.coden.database.asInstant
import io.github.coden.database.transaction
import io.github.coden.utils.flatMap
import io.github.coden.utils.randomPronouncable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class AnxietyDatabaseRepository(private val db: Database) : AnxietyRepository {


    override fun saveAnxiety(anxiety: Anxiety): Result<Anxiety> = db.transaction {
        Anxieties.insert {
            it[id] = anxiety.id
            it[created] = anxiety.created.asDBInstant()
            it[description] = anxiety.description
        }
        anxiety
    }

    override fun saveResolution(resolution: Resolution): Result<Resolution> = db.transaction {
        Resolutions.insert {
            it[anxietyId] = resolution.anxietyId
            it[created] = resolution.created.asDBInstant()
            it[fulfilled] = resolution.fulfilled
        }
        resolution
    }

    override fun saveChanceAssessment(assessment: ChanceAssessment): Result<ChanceAssessment> = db.transaction {
        ChanceAssessments.insert {
            it[anxietyId] = assessment.anxietyId
            it[chance] = assessment.chance.level
            it[id] = assessment.id
            it[created] = assessment.created.asDBInstant()
        }
        assessment
    }

    override fun updateAnxiety(anxiety: Anxiety): Result<Anxiety> = db.transaction {
        Anxieties.update(where = { Anxieties.id eq anxiety.id }) {
            it[description] = anxiety.description
        }
    }.flatMap {
        getAnxietyById(anxiety.id)
    }

    override fun updateResolution(resolution: Resolution): Result<Resolution> = db.transaction {
        Resolutions.update(where = { Resolutions.anxietyId eq resolution.anxietyId }) {
            it[fulfilled] = resolution.fulfilled
            it[created] = resolution.created.asDBInstant()
        }

        getResolutionById(resolution.anxietyId)
    }

    private fun getResolutionById(anxietyId: String): Resolution {
        return Resolutions
            .selectAll()
            .where { Resolutions.anxietyId eq anxietyId }
            .mapNotNull { mapResolution(it) }
            .single()
    }

    override fun deleteAnxietyById(anxietyId: String): Result<Anxiety> = db.transaction {
        val anxiety = getAnxietyById(anxietyId)
        Resolutions.deleteWhere { Resolutions.anxietyId eq anxietyId }
        ChanceAssessments.deleteWhere { ChanceAssessments.anxietyId eq anxietyId }
        Anxieties.deleteWhere { id eq anxietyId }
        anxiety.getOrThrow()
    }

    override fun deleteResolutionByAnxietyId(anxietyId: String): Result<Resolution> = db.transaction {
        val resolution = getResolutionById(anxietyId)
        Resolutions.deleteWhere { Resolutions.anxietyId eq anxietyId }
        resolution
    }

    override fun deleteChanceAssessmentById(assessmentId: String): Result<ChanceAssessment> = db.transaction {
        val assessment = getChanceAssessmentById(assessmentId)
        ChanceAssessments.deleteWhere { id eq assessmentId }
        assessment
    }

    private fun getChanceAssessmentById(id: String): ChanceAssessment {
        return ChanceAssessments
            .selectAll()
            .where { ChanceAssessments.id eq id }
            .mapNotNull { mapChanceAssessment(it) }
            .single()
    }

    override fun clearAnxieties(): Result<Long> = db.transaction {
        Anxieties.deleteAll().toLong()
    }

    override fun clearResolutions(): Result<Long> = db.transaction {
        Resolutions.deleteAll().toLong()
    }

    override fun clearChanceAssessments(): Result<Long> = db.transaction {
        ChanceAssessments.deleteAll().toLong()
    }

    override fun getNextAnxietyId(): Result<String> {
        return Result.success(randomPronouncable(3, 5))
    }

    override fun getNextChanceAssessmentId(anxietyId: String): Result<String> = db.transaction {
        ChanceAssessments
            .selectAll()
            .where { ChanceAssessments.anxietyId eq anxietyId }
            .count()
            .toString()
    }

    override fun getAnxietyById(anxietyId: String): Result<Anxiety> = db.transaction {
        Anxieties
            .leftJoin(Resolutions, { Anxieties.id }, { Resolutions.anxietyId })
            .leftJoin(ChanceAssessments, { Anxieties.id }, { ChanceAssessments.anxietyId })
            .selectAll()
            .where { Anxieties.id eq anxietyId }
            .groupBy { mapAnxiety(it) }
            .map { (key, value) ->
                key.copy(chanceAssessments = value.mapNotNull { mapChanceAssessment(it) })
            }
            .singleOrNull() ?: throw NoSuchAnxietyException(anxietyId)
    }

    override fun getAnxieties(): Result<List<Anxiety>> = db.transaction {
        Anxieties
            .leftJoin(Resolutions, { Anxieties.id }, { Resolutions.anxietyId })
            .leftJoin(ChanceAssessments, { Anxieties.id }, { ChanceAssessments.anxietyId })
            .selectAll()
            .groupBy { mapAnxiety(it) }
            .map { (key, value) ->
                key.copy(chanceAssessments = value.mapNotNull { mapChanceAssessment(it) })
            }
    }

    private fun mapAnxiety(row: ResultRow): Anxiety {
        return Anxiety(
            description = row[Anxieties.description],
            id = row[Anxieties.id],
            created = row[Anxieties.created].asInstant(),
            resolution = mapResolution(row),
            chanceAssessments = listOf()
        )
    }

    private fun mapResolution(row: ResultRow): Resolution? {
        val anxietyId = row.getOrNull(Resolutions.anxietyId) ?: return null
        return Resolution(
            anxietyId,
            row[Resolutions.fulfilled],
            row[Resolutions.created].asInstant()
        )
    }

    private fun mapChanceAssessment(row: ResultRow): ChanceAssessment? {
        val anxietyId = row.getOrNull(ChanceAssessments.anxietyId) ?: return null
        return ChanceAssessment(
            anxietyId,
            row[ChanceAssessments.chance].chance(),
            row[ChanceAssessments.id],
            row[ChanceAssessments.created].asInstant(),
        )
    }

}