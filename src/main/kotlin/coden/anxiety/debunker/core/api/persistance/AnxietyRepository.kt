package coden.anxiety.debunker.core.api.persistance

interface AnxietyRepository {
    fun list(): Result<List<Anxiety>>
    fun insert(anxiety: Anxiety): Result<Unit>
    fun delete(anxietyId: String): Result<Unit>
    fun get(anxietyId: String): Result<Anxiety>
    fun update(anxietyId: String, description: String): Result<Anxiety>
    fun clear(): Result<Long>
}

class NoAnxietyException(val anxietyId: String, message: String) : Exception(message)

