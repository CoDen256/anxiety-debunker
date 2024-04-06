package coden.anxiety.debunker.core.api

import java.nio.file.DirectoryStream.Filter
import java.time.Instant

interface AnxietyAnalyser {
    fun anxiety(request: AnxietyRequest): Result<AnxietyEntityResponse>
    fun anxieties(request: ListAnxietiesRequest): Result<AnxietyListResponse>
}

interface AnxietyAnalyserRequest

data class AnxietyRequest(
    val id: String
): AnxietyAnalyserRequest

data class ListAnxietiesRequest(
    val filter: AnxietyFilter
): AnxietyAnalyserRequest

enum class AnxietyFilter
    (filter: Filter<AnxietyEntityResolution>) : Filter<AnxietyEntityResolution> by filter
{
    FULLFILLED({it == AnxietyEntityResolution.FULFILLED }),
    UNFULLFILLED({it == AnxietyEntityResolution.UNFULFILLED }),
    UNRESOLVED({it == AnxietyEntityResolution.UNRESOLVED }),
    ALL({true})
}

interface AnxietyAnalyserResponse

data class AnxietyEntityResponse(
    val id: String,
    val description: String,
    val created: Instant,
    val resolution: AnxietyEntityResolution,
    val resolvedAt: Instant?,
): AnxietyAnalyserResponse

enum class AnxietyEntityResolution{ FULFILLED, UNFULFILLED, UNRESOLVED }


data class AnxietyListResponse(
    val anxieties: List<AnxietyEntityResponse>
): AnxietyAnalyserResponse