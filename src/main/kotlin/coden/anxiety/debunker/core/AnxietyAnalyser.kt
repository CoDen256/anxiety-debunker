package coden.anxiety.debunker.core

import java.nio.file.DirectoryStream.Filter
import java.time.Instant

interface AnxietyAnalyser {
    fun anxiety(request: NewAnxietyRequest): Result<AnxietyEntityResponse>
    fun anxieties(request: ListAnxietyRequest): Result<AnxietyListResponse>

    fun resolution(request: ResolutionRequest): Result<ResolutionEntityResponse>
    fun resolutions(request: ListResolutionRequest): Result<ResolutionListResponse>
}


interface AnxietyAnalyserRequest

data class AnxietyRequest(
    val id: String
): AnxietyAnalyserRequest

data class ListAnxietyRequest(
    val filter: AnxietyFilter
): AnxietyAnalyserRequest

enum class AnxietyFilter
    (filter: Filter<AnxietyResolution>) : Filter<AnxietyResolution> by filter
{
    FULLFILLED({it == AnxietyResolution.FULFILLED}),
    UNFULLFILLED({it == AnxietyResolution.UNFULFILLED}),
    UNRESOLVED({it == AnxietyResolution.UNRESOLVED}),
    ALL({true})
}

data class ResolutionRequest(
    val anxietyId: String,
): AnxietyAnalyserRequest

data object ListResolutionRequest : AnxietyAnalyserRequest

interface AnxietyAnalyserResponse

data class AnxietyEntityResponse(
    val id: String,
    val description: String,
    val resolution: AnxietyResolution,
    val created: Instant,
): AnxietyAnalyserResponse

enum class AnxietyResolution{
    FULFILLED, UNFULFILLED, UNRESOLVED
}

data class AnxietyListResponse(
    val anxieties: List<AnxietyEntityResponse>
): AnxietyAnalyserResponse

data class ResolutionEntityResponse(
    val resolvedAt: Instant,
    val anxietyId: String,
    val fulfilled: Boolean
): AnxietyAnalyserResponse

data class ResolutionListResponse(
    val resolutions: List<ResolutionEntityResponse>
): AnxietyAnalyserResponse