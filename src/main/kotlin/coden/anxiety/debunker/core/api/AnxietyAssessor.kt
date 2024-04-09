package coden.anxiety.debunker.core.api

import coden.anxiety.debunker.core.persistance.RiskLevel

interface AnxietyAssessor {
    fun add(request: NewRiskRequest): Result<NewRiskResponse>
    fun remove(request: DeleteRiskRequest): Result<DeleteRiskResponse>
    fun clear(request: ClearRisksRequest): Result<ClearRisksResponse>
}

sealed interface AssessmentRequest
data class NewRiskRequest(
    val level: RiskLevel,
    val anxietyId: String,
) : AssessmentRequest

data class DeleteRiskRequest(
    val id: String,
) : AssessmentRequest
data object ClearRisksRequest : AssessmentRequest


sealed interface AssessmentResponse
data class NewRiskResponse(
    val id: String,
    val anxietyId: String,
    val level: RiskLevel,
) : AssessmentResponse {}

data class DeleteRiskResponse(
    val id: String
) : AssessmentResponse {}

data class ClearRisksResponse(
    val count: Long
) : AssessmentResponse {}
