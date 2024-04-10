package coden.anxiety.debunker.core.persistance

interface AnxietyRepository {
    fun saveAnxiety(anxiety: AnxietyEntity): Result<Unit>
    fun saveResolution(resolution: Resolution): Result<Unit>
    fun saveRiskAssessment(assessment: RiskAssessment): Result<Unit>

    fun updateAnxiety(anxietyId: String, description: String): Result<AnxietyEntity>
    fun updateResolution(resolution: Resolution): Result<Resolution>

    fun deleteAnxiety(anxietyId: String): Result<Unit>
    fun deleteResolution(anxietyId: String): Result<Unit>
    fun deleteRiskAssessment(assessmentId: String): Result<Unit>

    fun clearAnxieties(): Result<Long>
    fun clearResolutions(): Result<Long>
    fun clearRiskAssessments(): Result<Long>

    fun anxiety(anxietyId: String): Result<FullAnxietyEntity>
    fun anxieties(): Result<List<FullAnxietyEntity>>
}

class NoSuchAnxietyException(val anxietyId: String, message: String) : Exception(message)