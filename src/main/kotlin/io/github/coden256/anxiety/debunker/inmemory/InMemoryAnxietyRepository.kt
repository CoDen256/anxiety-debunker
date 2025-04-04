package io.github.coden256.anxiety.debunker.inmemory

import io.github.coden256.anxiety.debunker.core.persistance.*
import io.github.coden256.utils.notNullOrFailure
import io.github.coden256.utils.success

class InMemoryAnxietyRepository : AnxietyRepository {

    private val anxieties: MutableMap<String, Anxiety> = HashMap()
    private val resolutions: MutableMap<String, Resolution> = HashMap()
    private val chanceAssessments: MutableMap<String, ChanceAssessment> = HashMap()
    private val anxietyDetails: MutableMap<String, AnxietyDetail> = HashMap()

    override fun saveAnxiety(anxiety: Anxiety): Result<Anxiety> {
        anxieties[anxiety.id] = anxiety
        return anxiety.success()
    }

    override fun saveResolution(resolution: Resolution): Result<Resolution> {
        resolutions[resolution.anxietyId] = resolution
        return resolution.success()
    }

    override fun saveChanceAssessment(assessment: ChanceAssessment): Result<ChanceAssessment> {
        chanceAssessments[assessment.id] = assessment
        return assessment.success()
    }

    override fun saveDetail(detail: AnxietyDetail): Result<AnxietyDetail> {
        anxietyDetails[detail.anxietyId] = detail
        return detail.success()
    }

    override fun updateAnxiety(anxiety: Anxiety): Result<Anxiety> {
        if (!anxieties.containsKey(anxiety.id)) return Result.failure(NoSuchAnxietyException(anxiety.id))

        return Result.success(anxieties.compute(anxiety.id) { _, _ ->
            anxiety
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
        return Result.success(resolutions.compute(resolution.anxietyId) { _, _ ->
            resolution
        }!!)
    }

    override fun updateDetail(detail: AnxietyDetail): Result<AnxietyDetail> {
        if (!anxietyDetails.containsKey(detail.anxietyId)) return Result.failure(NoSuchAnxietyException(detail.anxietyId))

        return Result.success(anxietyDetails.compute(detail.anxietyId) {_, _ ->
            detail
        }!!)
    }

    override fun deleteAnxietyById(anxietyId: String): Result<Anxiety> {
        return anxieties.remove(anxietyId).notNullOrFailure(NoSuchAnxietyException(anxietyId))
    }

    override fun deleteResolutionByAnxietyId(anxietyId: String): Result<Resolution> {
        return resolutions.remove(anxietyId).notNullOrFailure(NoSuchAnxietyException(anxietyId))
    }

    override fun deleteChanceAssessmentById(assessmentId: String): Result<ChanceAssessment> {
        return chanceAssessments.remove(assessmentId).notNullOrFailure(NoSuchAnxietyException(assessmentId))
    }

    override fun deleteDetailByAnxietyId(anxietyId: String): Result<AnxietyDetail> {
        return anxietyDetails.remove(anxietyId).notNullOrFailure(NoSuchAnxietyException(anxietyId))
    }

    override fun clearAnxieties(): Result<Long> {
        val size = anxieties.size.toLong()
        anxieties.clear()
        return Result.success(size)
    }

    override fun clearResolutions(): Result<Long> {
        val size = resolutions.size.toLong()
        resolutions.clear()
        return Result.success(size)
    }

    override fun clearChanceAssessments(): Result<Long> {
        val size = chanceAssessments.size.toLong()
        chanceAssessments.clear()
        return Result.success(size)
    }

    override fun clearDetails(): Result<Long> {
        val size = anxietyDetails.size.toLong()
        anxietyDetails.clear()
        return Result.success(size)
    }

    override fun getNextAnxietyId(): Result<String> {
        return Result.success("mem-${anxieties.size}")
    }

    override fun getNextChanceAssessmentId(anxietyId: String): Result<String> {
        return Result.success("mem-${chanceAssessments.size}")
    }

    override fun getAnxietyById(anxietyId: String): Result<Anxiety> {
        if (!anxieties.containsKey(anxietyId)) {
            return Result.failure(NoSuchAnxietyException(anxietyId, "No anxiety with id $anxietyId"))
        }
        val anxiety = anxieties[anxietyId]!!
        val resolution = resolutions[anxiety.id]
        val assessment = chanceAssessments.map { it.value }.filter { it.anxietyId == anxietyId }
        val detail = anxietyDetails[anxiety.id]

        return Result.success(
            Anxiety(
                anxiety.id,
                anxiety.description,
                anxiety.created,
                resolution,
                detail,
                assessment,
            )
        )
    }

    override fun getAnxieties(): Result<List<Anxiety>> {
        return Result.success(anxieties.map {
            getAnxietyById(it.key).getOrThrow()
        })
    }
}