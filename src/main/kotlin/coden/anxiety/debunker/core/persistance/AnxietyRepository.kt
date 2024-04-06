package coden.anxiety.debunker.core.persistance

interface AnxietyRepository {
    fun saveAnxiety(anxiety: Anxiety): Result<Unit>
    fun saveResolution(resolution: Resolution): Result<Unit>

    fun updateAnxiety(anxietyId: String, description: String): Result<Anxiety>
    fun updateResolution(resolution: Resolution): Result<Resolution>

    fun deleteAnxiety(anxietyId: String): Result<Unit>
    fun deleteResolution(anxietyId: String): Result<Unit>

    fun clearAnxieties(): Result<Long>
    fun clearResolutions(): Result<Long>

    fun anxiety(anxietyId: String): Result<AnxietyWithResolution>
    fun anxieties(): Result<List<AnxietyWithResolution>>
}

class NoSuchAnxietyException(val anxietyId: String, message: String) : Exception(message)