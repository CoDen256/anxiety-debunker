package coden.anxiety.debunker.postgres

import coden.anxiety.debunker.core.persistance.*
import coden.anxiety.debunker.core.persistance.RiskLevel.Companion.asRisk
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class AnxietyDatabaseRepository(private val db: Database) : AnxietyRepository {

    private fun <T : Any> transaction(query: Transaction.() -> T): Result<T> {
        return try {
            Result.success(transaction(db) { query() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun saveAnxiety(anxiety: AnxietyEntity): Result<Unit> = transaction {
        Anxieties.insert {
            it[id] = anxiety.id
            it[created] = anxiety.created.asDBInstant()
            it[description] = anxiety.description
        }
    }

    override fun saveResolution(resolution: Resolution): Result<Unit> = transaction {
        Resolutions.insert {
            it[anxietyId] = resolution.anxietyId
            it[resolved] = resolution.resolvedAt.asDBInstant()
            it[fulfilled] = resolution.fulfilled
        }
    }

    override fun saveRiskAssessment(assessment: RiskAssessment): Result<Unit> = transaction {
        RiskAssessments.insert {
            it[anxietyId] = assessment.anxietyId
            it[risk] = assessment.risk.level
            it[id] = assessment.id
            it[assessed] = assessment.assessed.asDBInstant()
        }
    }

    override fun updateAnxiety(anxietyId: String, newDescription: String): Result<AnxietyEntity> = transaction {
        Anxieties.update(where = { Anxieties.id eq anxietyId }){
            it[description] = newDescription
        }
        Anxieties
            .selectAll()
            .where { Anxieties.id eq anxietyId }
            .single()
            .let { AnxietyEntity(it[Anxieties.description], it[Anxieties.id], it[Anxieties.created].asInstant()) }
    }

    override fun updateResolution(resolution: Resolution): Result<Resolution> = transaction {
        Resolutions.update(where = { Resolutions.anxietyId eq resolution.anxietyId }){
            it[fulfilled] = resolution.fulfilled
            it[resolved] = resolution.resolvedAt.asDBInstant()
        }

        Resolutions
            .selectAll()
            .where { Resolutions.anxietyId eq resolution.anxietyId }
            .single()
            .let { Resolution(it[Resolutions.anxietyId], it[Resolutions.fulfilled], it[Resolutions.resolved].asInstant()) }
    }

    override fun deleteAnxiety(anxietyId: String): Result<Unit> = transaction {
        Resolutions.deleteWhere { Resolutions.anxietyId eq anxietyId }
        RiskAssessments.deleteWhere { RiskAssessments.anxietyId eq anxietyId }
        Anxieties.deleteWhere { id eq anxietyId }
    }

    override fun deleteResolution(anxietyId: String): Result<Unit> = transaction {
        Resolutions.deleteWhere { Resolutions.anxietyId eq anxietyId }
    }

    override fun deleteRiskAssessment(assessmentId: String): Result<Unit> = transaction {
        RiskAssessments.deleteWhere { id eq assessmentId }
    }

    override fun clearAnxieties(): Result<Long> = transaction {
        Anxieties.deleteAll().toLong()
    }

    override fun clearResolutions(): Result<Long> = transaction {
        Resolutions.deleteAll().toLong()
    }

    override fun clearRiskAssessments(): Result<Long> = transaction {
        RiskAssessments.deleteAll().toLong()
    }

    override fun anxiety(anxietyId: String): Result<FullAnxietyEntity> = transaction {
        Anxieties
            .leftJoin(Resolutions, { Anxieties.id }, { Resolutions.anxietyId })
            .leftJoin(RiskAssessments, { Anxieties.id }, { RiskAssessments.anxietyId })
            .selectAll()
            .where { Anxieties.id eq anxietyId }
            .groupBy { mapAnxiety(it) }
            .map { (key, value) ->
                key.copy(riskAssessments = value.mapNotNull { mapRisk(it) })
            }
            .single()
    }

    override fun anxieties(): Result<List<FullAnxietyEntity>> = transaction {
        Anxieties
            .leftJoin(Resolutions, { Anxieties.id }, { Resolutions.anxietyId })
            .leftJoin(RiskAssessments, { Anxieties.id }, { RiskAssessments.anxietyId })
            .selectAll()
            .groupBy { mapAnxiety(it) }
            .map { (key, value) ->
                key.copy(riskAssessments = value.mapNotNull { mapRisk(it) })
            }
    }

    private fun mapAnxiety(row: ResultRow): FullAnxietyEntity {
        return FullAnxietyEntity(
            row[Anxieties.id],
            row[Anxieties.description],
            row[Anxieties.created].asInstant(),
            mapResolution(row),
            listOf()
        )
    }

    private fun mapResolution(row: ResultRow): Resolution? {
        val anxietyId = row.getOrNull(Resolutions.anxietyId) ?: return null
        return Resolution(
            anxietyId,
            row[Resolutions.fulfilled],
            row[Resolutions.resolved].asInstant()
        )
    }

    private fun mapRisk(row: ResultRow): RiskAssessment? {
        val anxietyId = row.getOrNull(RiskAssessments.anxietyId) ?: return null
        return RiskAssessment(
            anxietyId,
            row[RiskAssessments.risk].asRisk(),
            row[RiskAssessments.assessed].asInstant(),
            row[RiskAssessments.id],
        )
    }

}