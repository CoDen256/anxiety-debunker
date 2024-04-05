package coden.anxiety.debunker.core

import java.nio.file.DirectoryStream.Filter
import java.time.Instant

interface AnxietyHolder {
    fun add(request: NewAnxietyRequest): Result<NewAnxietyResponse>
    fun delete(request: DeleteAnxietyRequest): Result<DeleteAnxietyResponse>
    fun update(request: UpdateAnxietyRequest): Result<UpdateAnxietyResponse>
    fun list(request: ListAnxietyRequest): Result<ListAnxietyResponse>
    fun clear(request: ClearAnxietiesRequest): Result<ClearAnxietiesResponse>
}


sealed interface AnxietyHolderRequest

data class NewAnxietyRequest(
    val description: String
    // level
): AnxietyHolderRequest

data class DeleteAnxietyRequest(
    val id: String
): AnxietyHolderRequest

data class UpdateAnxietyRequest(
    val id: String,
    val description: String
): AnxietyHolderRequest

data class ListAnxietyRequest(
    val filter: AnxietyFilter
): AnxietyHolderRequest

enum class AnxietyFilter
    (filter: Filter<AnxietyResolution>) : Filter<AnxietyResolution> by filter
{
    FULLFILLED({it == AnxietyResolution.FULFILLED}),
    UNFULLFILLED({it == AnxietyResolution.UNFULFILLED}),
    UNRESOLVED({it == AnxietyResolution.UNRESOLVED}),
    ALL({true})
}

data object ClearAnxietiesRequest: AnxietyHolderRequest



sealed interface AnxietyHolderResponse

data class NewAnxietyResponse(
    val id: String,
    val description: String,
    val created: Instant
): AnxietyHolderResponse

data class DeleteAnxietyResponse(
    val id: String,
): AnxietyHolderResponse

data class UpdateAnxietyResponse(
    val id: String,
    val description: String,
    val created: Instant
): AnxietyHolderResponse

data class AnxietyEntity(
    val id: String,
    val description: String,
    val resolution: AnxietyResolution,
    val created: Instant,
)

enum class AnxietyResolution{
    FULFILLED, UNFULFILLED, UNRESOLVED
}

data class ListAnxietyResponse(
    val anxieties: List<AnxietyEntity>
): AnxietyHolderResponse

data class ClearAnxietiesResponse(
    val count: Long
): AnxietyHolderResponse