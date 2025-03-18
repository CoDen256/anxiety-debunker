package io.github.coden.anxiety.debunker.core.api

interface AnxietyDetailEditor {
    fun add(request: NewDetailRequest): Result<NewDetailResponse>
    fun update(request: UpdateDetailRequest): Result<UpdateDetailResponse>
    fun remove(request: DeleteDetailRequest): Result<DeleteDetailResponse>
    fun clear(request: ClearDetailsRequest): Result<ClearDetailsResponse>
}

sealed interface DetailRequest
data class NewDetailRequest(val anxietyId: String, val trigger: String, val bodyResponse: String, val anxietyResponse: String, val alternativeThoughts: String) : DetailRequest
data class UpdateDetailRequest(val anxietyId: String, val trigger: String, val bodyResponse: String, val anxietyResponse: String, val alternativeThoughts: String): AnxietyHolderRequest
data class DeleteDetailRequest(val anxietyId: String) : DetailRequest
data object ClearDetailsRequest : DetailRequest


sealed interface DetailResponse
data class NewDetailResponse(val anxietyId: String, val trigger: String, val bodyResponse: String, val anxietyResponse: String, val alternativeThoughts: String) : DetailResponse {}
data class UpdateDetailResponse(val anxietyId: String, val trigger: String, val bodyResponse: String, val anxietyResponse: String, val alternativeThoughts: String): AnxietyHolderResponse
data class DeleteDetailResponse(val anxietyId: String) : DetailResponse
data class ClearDetailsResponse(val count: Long) : DetailResponse
