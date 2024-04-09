package coden.anxiety.debunker.core.api

import coden.anxiety.debunker.core.persistance.RiskLevel
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
    val filter: (AnxietyEntityResponse) -> Boolean
): AnxietyAnalyserRequest

enum class AnxietyFilter
    (filter: (AnxietyEntityResponse) -> Boolean) : (AnxietyEntityResponse) -> Boolean by filter
{
    FULLFILLED({it.resolution == AnxietyEntityResolution.FULFILLED }),
    UNFULLFILLED({it.resolution == AnxietyEntityResolution.UNFULFILLED }),
    UNRESOLVED({it.resolution == AnxietyEntityResolution.UNRESOLVED }),
    MAX_RISK({it.risk == RiskLevel.MAX}),
    ALL({true});

    operator fun times(filter: AnxietyFilter): (AnxietyEntityResponse) -> Boolean{
        return {this.invoke(it) && filter(it)}
    }
    operator fun plus(filter: AnxietyFilter): (AnxietyEntityResponse) -> Boolean{
        return {this.invoke(it) || filter(it)}
    }
    operator fun not(): (AnxietyEntityResponse) -> Boolean{
        return {!this(it)}
    }
}

interface AnxietyAnalyserResponse

data class AnxietyEntityResponse(
    val id: String,
    val description: String,
    val created: Instant,
    val risk: RiskLevel?,
    val resolution: AnxietyEntityResolution,
    val resolvedAt: Instant?
    ,
): AnxietyAnalyserResponse

enum class AnxietyEntityResolution{ FULFILLED, UNFULFILLED, UNRESOLVED }

data class AnxietyListResponse(
    val anxieties: List<AnxietyEntityResponse>
): AnxietyAnalyserResponse