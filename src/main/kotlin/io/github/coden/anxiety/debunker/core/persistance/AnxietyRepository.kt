package io.github.coden.anxiety.debunker.core.persistance

interface AnxietyRepository {
    fun saveAnxiety(anxiety: Anxiety): Result<Anxiety>
    fun saveResolution(resolution: Resolution): Result<Resolution>
    fun saveChanceAssessment(assessment: ChanceAssessment): Result<ChanceAssessment>
    fun saveDetail(detail: AnxietyDetail): Result<AnxietyDetail>

    fun updateAnxiety(anxiety: Anxiety): Result<Anxiety>
    fun updateResolution(resolution: Resolution): Result<Resolution>
    fun updateDetail(detail: AnxietyDetail): Result<AnxietyDetail>

    fun deleteAnxietyById(anxietyId: String): Result<Anxiety>
    fun deleteResolutionByAnxietyId(anxietyId: String): Result<Resolution>
    fun deleteChanceAssessmentById(assessmentId: String): Result<ChanceAssessment>
    fun deleteDetailByAnxietyId(anxietyId: String): Result<AnxietyDetail>

    fun clearAnxieties(): Result<Long>
    fun clearResolutions(): Result<Long>
    fun clearChanceAssessments(): Result<Long>
    fun clearDetails(): Result<Long>

    fun getNextAnxietyId(): Result<String>
    fun getNextChanceAssessmentId(anxietyId: String): Result<String>
    fun getAnxietyById(anxietyId: String): Result<Anxiety>
    fun getAnxieties(): Result<List<Anxiety>>
}

class NoSuchAnxietyException(val id: String, message: String="No anxiety with id $id") : Exception(message)