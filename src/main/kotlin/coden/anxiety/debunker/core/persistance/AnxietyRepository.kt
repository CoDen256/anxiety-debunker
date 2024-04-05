package coden.anxiety.debunker.core.persistance

interface AnxietyRepository {
    fun list(): Result<List<Anxiety>>
    fun insert(anxiety: Anxiety): Result<Unit>
    fun delete(anxietyId: String): Result<Unit>
    fun update(anxiety: Anxiety): Result<Anxiety>
    fun clear(): Result<Unit>
}

