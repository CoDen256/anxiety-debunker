package io.github.coden256.anxiety.debunker.core.api

import java.time.Instant

interface AnxietyAnalyser {
    fun anxiety(request: io.github.coden256.anxiety.debunker.core.api.GetAnxietyRequest): Result<io.github.coden256.anxiety.debunker.core.api.AnxietyEntityResponse>
    fun anxieties(request: io.github.coden256.anxiety.debunker.core.api.ListAnxietiesRequest): Result<io.github.coden256.anxiety.debunker.core.api.AnxietyListResponse>
}

sealed interface AnxietyAnalyserRequest

data class GetAnxietyRequest(val id: String) : io.github.coden256.anxiety.debunker.core.api.AnxietyAnalyserRequest
data class ListAnxietiesRequest(val resolutions: io.github.coden256.anxiety.debunker.core.api.ResolutionFilter = io.github.coden256.anxiety.debunker.core.api.ResolutionFilter.Companion.ALL,
                                val chances: io.github.coden256.anxiety.debunker.core.api.ChanceFilter = io.github.coden256.anxiety.debunker.core.api.ChanceFilter.Companion.ALL
) : io.github.coden256.anxiety.debunker.core.api.AnxietyAnalyserRequest

class ResolutionFilter
    (filter: (io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionResponse?) -> Boolean) :
        (io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionResponse?) -> Boolean by filter {

    companion object {
        val FULLFILLED =
            io.github.coden256.anxiety.debunker.core.api.ResolutionFilter { it?.type == io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionType.FULFILLED }
        val UNFULLFILLED =
            io.github.coden256.anxiety.debunker.core.api.ResolutionFilter { it?.type == io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionType.UNFULFILLED }
        val UNRESOLVED =
            io.github.coden256.anxiety.debunker.core.api.ResolutionFilter { it?.type == io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionType.UNRESOLVED }
        val ALL = io.github.coden256.anxiety.debunker.core.api.ResolutionFilter { true }
    }
    operator fun times(filter: io.github.coden256.anxiety.debunker.core.api.ResolutionFilter): io.github.coden256.anxiety.debunker.core.api.ResolutionFilter {
        return io.github.coden256.anxiety.debunker.core.api.ResolutionFilter { this.invoke(it) && filter(it) }
    }

    operator fun plus(filter: io.github.coden256.anxiety.debunker.core.api.ResolutionFilter): io.github.coden256.anxiety.debunker.core.api.ResolutionFilter {
        return io.github.coden256.anxiety.debunker.core.api.ResolutionFilter { this.invoke(it) || filter(it) }
    }

    operator fun not(): io.github.coden256.anxiety.debunker.core.api.ResolutionFilter {
        return io.github.coden256.anxiety.debunker.core.api.ResolutionFilter { !this.invoke(it) }
    }
}

class ChanceFilter(
    filter: (io.github.coden256.anxiety.debunker.core.api.AnxietyChanceAssessmentResponse?) -> Boolean
): (io.github.coden256.anxiety.debunker.core.api.AnxietyChanceAssessmentResponse?) -> Boolean by filter
{
    companion object {
        const val LOWEST_LEVEL = 0
        const val HIGHEST_LEVEL = 100
        val LOWEST_CHANCE =
            io.github.coden256.anxiety.debunker.core.api.ChanceFilter { it?.level == io.github.coden256.anxiety.debunker.core.api.ChanceFilter.Companion.LOWEST_LEVEL }
        val HIGHEST_CHANCE =
            io.github.coden256.anxiety.debunker.core.api.ChanceFilter { it?.level == io.github.coden256.anxiety.debunker.core.api.ChanceFilter.Companion.HIGHEST_LEVEL }
        val ALL = io.github.coden256.anxiety.debunker.core.api.ChanceFilter { true }
    }
}

sealed interface AnxietyAnalyserResponse

data class AnxietyEntityResponse(
    val id: String,
    val description: String,
    val created: Instant,
    val chances: List<io.github.coden256.anxiety.debunker.core.api.AnxietyChanceAssessmentResponse>,
    val resolution: io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionResponse,
    val details: io.github.coden256.anxiety.debunker.core.api.AnxietyDetailResponse?,
) : io.github.coden256.anxiety.debunker.core.api.AnxietyAnalyserResponse {
    fun latestChanceAssessment(): io.github.coden256.anxiety.debunker.core.api.AnxietyChanceAssessmentResponse? {
        return chances.lastOrNull()
    }
}

data class AnxietyChanceAssessmentResponse(val level: Int, val created: Instant) :
    io.github.coden256.anxiety.debunker.core.api.AnxietyAnalyserResponse
data class AnxietyResolutionResponse(val type: io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionType, val resolved: Instant?) :
    io.github.coden256.anxiety.debunker.core.api.AnxietyAnalyserResponse
data class AnxietyDetailResponse(val trigger: String, val bodyResponse: String, val anxietyResponse: String, val alternativeThoughts: String) :
    io.github.coden256.anxiety.debunker.core.api.AnxietyAnalyserResponse
enum class AnxietyResolutionType { FULFILLED, UNFULFILLED, UNRESOLVED }
data class AnxietyListResponse(val anxieties: List<io.github.coden256.anxiety.debunker.core.api.AnxietyEntityResponse>) :
    io.github.coden256.anxiety.debunker.core.api.AnxietyAnalyserResponse