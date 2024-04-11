package coden.anxiety.debunker.core.api

interface AnxietyAssessor {
    fun assess(request: NewChanceAssessmentRequest): Result<NewChanceAssessmentResponse>
    fun remove(request: DeleteChanceAssessmentRequest): Result<DeleteChanceAssessmentResponse>
    fun clear(request: ClearChanceAssessmentsRequest): Result<ClearChanceAssessmentsResponse>
}

sealed interface AssessmentRequest
data class NewChanceAssessmentRequest(val chance: ChanceAssessment, val anxietyId: String) : AssessmentRequest
data class DeleteChanceAssessmentRequest(val id: String) : AssessmentRequest
data object ClearChanceAssessmentsRequest : AssessmentRequest
data class ChanceAssessment(val level: Int){
    companion object {
        val HIGHEST = ChanceAssessment(100)
        val LOWEST = ChanceAssessment(0)
    }
}

sealed interface AssessmentResponse
data class NewChanceAssessmentResponse(val id: String, val anxietyId: String, val chance: Int) : AssessmentResponse {}
data class DeleteChanceAssessmentResponse(val id: String) : AssessmentResponse
data class ClearChanceAssessmentsResponse(val count: Long) : AssessmentResponse
