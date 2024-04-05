package coden.anxiety.debunker.core.persistance

interface ResolutionRespository {
    fun list(): Result<List<Resolution>>
    fun insert(resolution: Resolution): Result<Unit>
    fun delete(anxietyId: String): Result<Unit>
    fun update(resolution: Resolution): Result<Resolution>
    fun clear(): Result<Unit>
}