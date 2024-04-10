package coden.anxiety.debunker.inmemory

import coden.anxiety.debunker.core.persistance.*
import coden.utils.success

class InMemoryAnxietyRepository : AnxietyRepository {

    private val anxieties: MutableMap<String, AnxietyEntity> = HashMap()
    private val resolutions: MutableMap<String, Resolution> = HashMap()
    private val riskAssessments: MutableMap<String, RiskAssessment> = HashMap()

    override fun saveAnxiety(anxiety: AnxietyEntity): Result<Unit> {
        anxieties[anxiety.id] = anxiety
        return Unit.success()
    }

    override fun saveResolution(resolution: Resolution): Result<Unit> {
        resolutions[resolution.anxietyId] = resolution
        return Unit.success()
    }

    override fun saveRiskAssessment(assessment: RiskAssessment): Result<Unit> {
        riskAssessments[assessment.id] = assessment
        return Unit.success()
    }

    override fun updateAnxiety(anxietyId: String, description: String): Result<AnxietyEntity> {
        if (!anxieties.containsKey(anxietyId)) {
            return Result.failure(NoSuchAnxietyException(anxietyId, "No anxiety with id $anxietyId"))
        }
        return Result.success(anxieties.compute(anxietyId) { k, v ->
            v?.copy(description = description)
        }!!)
    }

    override fun updateResolution(resolution: Resolution): Result<Resolution> {
        if (!resolutions.containsKey(resolution.anxietyId)) {
            return Result.failure(
                NoSuchAnxietyException(
                    resolution.anxietyId,
                    "No anxiety with id ${resolution.anxietyId}"
                )
            )
        }
        return Result.success(resolutions.compute(resolution.anxietyId) { k, v ->
            resolution
        }!!)
    }

    override fun deleteAnxiety(anxietyId: String): Result<Unit> {
        anxieties.remove(anxietyId)
        return Unit.success()
    }

    override fun deleteResolution(anxietyId: String): Result<Unit> {
        resolutions.remove(anxietyId)
        return Result.success(Unit)
    }

    override fun deleteRiskAssessment(assessmentId: String): Result<Unit> {
        riskAssessments.remove(assessmentId)
        return Result.success(Unit)
    }

    override fun clearAnxieties(): Result<Long> {
        anxieties.clear()
        return Result.success(anxieties.size.toLong())
    }

    override fun clearResolutions(): Result<Long> {
        resolutions.clear()
        return Result.success(resolutions.size.toLong())
    }

    override fun clearRiskAssessments(): Result<Long> {
        riskAssessments.clear()
        return Result.success(riskAssessments.size.toLong())
    }

    override fun anxiety(anxietyId: String): Result<FullAnxietyEntity> {
        if (!anxieties.containsKey(anxietyId)) {
            return Result.failure(NoSuchAnxietyException(anxietyId, "No anxiety with id $anxietyId"))
        }
        val anxiety = anxieties[anxietyId]!!
        val resolution = resolutions[anxiety.id]
        val assessment = riskAssessments.map { it.value }.filter { it.anxietyId == anxietyId }

        return Result.success(
            FullAnxietyEntity(
                anxiety.id,
                anxiety.description,
                anxiety.created,
                resolution,
                assessment
            )
        )
    }

    override fun anxieties(): Result<List<FullAnxietyEntity>> {
        return Result.success(anxieties.map {
            anxiety(it.key).getOrThrow()
        })
    }
}