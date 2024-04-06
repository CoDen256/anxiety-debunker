package coden.anxiety.debunker.core.api.persistance

interface ResolutionRespository {
    fun list(): Result<List<Resolution>>
    fun insert(resolution: Resolution): Result<Unit>
    fun delete(anxietyId: String): Result<Unit>
    fun getResolution(anxietyId: String): Result<Resolution>
    fun getResolvedAnxiety(anxietyId: String): Result<ResolvedAnxiety>
    fun update(resolution: Resolution): Result<Resolution>
    fun clear(): Result<Long>
}

class NoResolutionException(val anxietyId: String, message: String) : Exception(message)
