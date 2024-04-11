package io.github.coden.anxiety.debunker.core.api

import java.time.Instant

interface AnxietyAnalyser {
    fun anxiety(request: GetAnxietyRequest): Result<AnxietyEntityResponse>
    fun anxieties(request: ListAnxietiesRequest): Result<AnxietyListResponse>
}

sealed interface AnxietyAnalyserRequest

data class GetAnxietyRequest(val id: String) : AnxietyAnalyserRequest
data class ListAnxietiesRequest(val resolutions: ResolutionFilter = ResolutionFilter.ALL,
                                val chances: ChanceFilter = ChanceFilter.ALL
) : AnxietyAnalyserRequest

class ResolutionFilter
    (filter: (AnxietyResolutionResponse?) -> Boolean) :
        (AnxietyResolutionResponse?) -> Boolean by filter {

    companion object {
        val FULLFILLED = ResolutionFilter { it?.type == AnxietyResolutionType.FULFILLED }
        val UNFULLFILLED = ResolutionFilter { it?.type == AnxietyResolutionType.UNFULFILLED }
        val UNRESOLVED = ResolutionFilter { it?.type == AnxietyResolutionType.UNRESOLVED }
        val ALL = ResolutionFilter { true }
    }
    operator fun times(filter: ResolutionFilter): ResolutionFilter {
        return ResolutionFilter{ this.invoke(it) && filter(it) }
    }

    operator fun plus(filter: ResolutionFilter): ResolutionFilter {
        return ResolutionFilter{ this.invoke(it) || filter(it) }
    }

    operator fun not(): ResolutionFilter {
        return ResolutionFilter{ !this.invoke(it) }
    }
}

class ChanceFilter(
    filter: (AnxietyChanceAssessmentResponse?) -> Boolean
): (AnxietyChanceAssessmentResponse?) -> Boolean by filter
{
    companion object {
        const val LOWEST_LEVEL = 0
        const val HIGHEST_LEVEL = 100
        val LOWEST_CHANCE = ChanceFilter { it?.level == LOWEST_LEVEL }
        val HIGHEST_CHANCE = ChanceFilter { it?.level == HIGHEST_LEVEL }
        val ALL = ChanceFilter { true }
    }
}

sealed interface AnxietyAnalyserResponse

data class AnxietyEntityResponse(
    val id: String,
    val description: String,
    val created: Instant,
    val chances: List<AnxietyChanceAssessmentResponse>,
    val resolution: AnxietyResolutionResponse,
) : AnxietyAnalyserResponse {
    fun latestChanceAssessment(): AnxietyChanceAssessmentResponse? {
        return chances.lastOrNull()
    }
}

data class AnxietyChanceAssessmentResponse(val level: Int, val created: Instant) : AnxietyAnalyserResponse
data class AnxietyResolutionResponse(val type: AnxietyResolutionType, val resolved: Instant?) : AnxietyAnalyserResponse
enum class AnxietyResolutionType { FULFILLED, UNFULFILLED, UNRESOLVED }
data class AnxietyListResponse(val anxieties: List<AnxietyEntityResponse>) : AnxietyAnalyserResponse