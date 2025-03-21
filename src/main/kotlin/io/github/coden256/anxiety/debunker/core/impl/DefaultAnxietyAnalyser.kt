package io.github.coden256.anxiety.debunker.core.impl

import io.github.coden256.anxiety.debunker.core.api.*
import io.github.coden256.anxiety.debunker.core.persistance.*
import io.github.coden256.anxiety.debunker.core.persistance.ChanceAssessment
import io.github.coden256.utils.logResult
import org.apache.logging.log4j.kotlin.Logging

class DefaultAnxietyAnalyser
    (
    private val anxietyRepository: AnxietyRepository,
) : io.github.coden256.anxiety.debunker.core.api.AnxietyAnalyser, Logging {
    override fun anxiety(request: io.github.coden256.anxiety.debunker.core.api.GetAnxietyRequest): Result<io.github.coden256.anxiety.debunker.core.api.AnxietyEntityResponse> {
        logger.info("Requesting anxiety ${request.id}...")

        return anxietyRepository
            .getAnxietyById(request.id)
            .map { mapAnxietyToEntityResponse(it) }
            .logResult(logger){ "Got anxiety ${it.id}..."}
    }

    override fun anxieties(request: io.github.coden256.anxiety.debunker.core.api.ListAnxietiesRequest): Result<io.github.coden256.anxiety.debunker.core.api.AnxietyListResponse> {
        logger.info("Requesting all anxieties...")

        return anxietyRepository
            .getAnxieties() // performing for now client side filtering
            .map { anxieties -> anxieties
                .map { mapAnxietyToEntityResponse(it) }
                .filter { anxiety -> request.chances.invoke(anxiety.latestChanceAssessment())  }
                .filter { anxiety -> request.resolutions.invoke(anxiety.resolution) }
            }
            .map { io.github.coden256.anxiety.debunker.core.api.AnxietyListResponse(it) }
            .logResult(logger){ "Got all anxieties"}
    }

    private fun mapAnxietyToEntityResponse(anxiety: Anxiety): io.github.coden256.anxiety.debunker.core.api.AnxietyEntityResponse {
        return io.github.coden256.anxiety.debunker.core.api.AnxietyEntityResponse(
            anxiety.id,
            anxiety.description,
            anxiety.created,
            anxiety.chanceAssessments.map { mapChanceAssessment(it) },
            mapResolution(anxiety.resolution),
            anxiety.detail?.let { mapDetail(it) }
        )
    }

    private fun mapResolution(resolution: Resolution?): io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionResponse {
        return when (resolution?.fulfilled) {
            true -> io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionResponse(
                io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionType.FULFILLED,
                resolution.created
            )
            false -> io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionResponse(
                io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionType.UNFULFILLED,
                resolution.created
            )
            else -> io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionResponse(
                io.github.coden256.anxiety.debunker.core.api.AnxietyResolutionType.UNRESOLVED,
                resolution?.created
            )
        }
    }

    private fun mapChanceAssessment(chance: ChanceAssessment): io.github.coden256.anxiety.debunker.core.api.AnxietyChanceAssessmentResponse {
        return io.github.coden256.anxiety.debunker.core.api.AnxietyChanceAssessmentResponse(
            chance.chance.level,
            chance.created
        )
    }

    private fun mapDetail(detail: AnxietyDetail): io.github.coden256.anxiety.debunker.core.api.AnxietyDetailResponse {
        return io.github.coden256.anxiety.debunker.core.api.AnxietyDetailResponse(
            detail.trigger,
            detail.bodyResponse,
            detail.anxietyResponse,
            detail.alternativeThoughts
        )
    }
}