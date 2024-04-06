package coden.anxiety.debunker.core.api

import java.time.Instant

interface AnxietyHolder {
    fun add(request: NewAnxietyRequest): Result<NewAnxietyResponse>
    fun delete(request: DeleteAnxietyRequest): Result<DeleteAnxietyResponse>
    fun update(request: UpdateAnxietyRequest): Result<UpdateAnxietyResponse>
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

data class ClearAnxietiesResponse(
    val count: Long
): AnxietyHolderResponse