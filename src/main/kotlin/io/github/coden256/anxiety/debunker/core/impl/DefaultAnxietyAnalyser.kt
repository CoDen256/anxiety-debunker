package io.github.coden256.anxiety.debunker.core.impl

import io.github.coden256.anxiety.debunker.core.api.*
import io.github.coden256.anxiety.debunker.core.persistance.*
import io.github.coden256.anxiety.debunker.core.persistance.ChanceAssessment
import io.github.coden256.utils.log
import org.apache.logging.log4j.kotlin.Logging

class DefaultAnxietyAnalyser
    (
    private val anxietyRepository: AnxietyRepository,
) : AnxietyAnalyser, Logging {
    override fun anxiety(request: GetAnxietyRequest): Result<AnxietyEntityResponse> {
        logger.info("Requesting anxiety ${request.id}...")

        return anxietyRepository
            .getAnxietyById(request.id)
            .map { mapAnxietyToEntityResponse(it) }
            .log(logger){ "Got anxiety ${it.id}..."}
    }

    override fun anxieties(request: ListAnxietiesRequest): Result<AnxietyListResponse> {
        logger.info("Requesting all anxieties...")

        return anxietyRepository
            .getAnxieties() // performing for now client side filtering
            .map { anxieties -> anxieties
                .map { mapAnxietyToEntityResponse(it) }
                .filter { anxiety -> request.chances.invoke(anxiety.latestChanceAssessment())  }
                .filter { anxiety -> request.resolutions.invoke(anxiety.resolution) }
            }
            .map { AnxietyListResponse(it) }
            .log(logger){ "Got all anxieties"}
    }

    private fun mapAnxietyToEntityResponse(anxiety: Anxiety): AnxietyEntityResponse {
        return AnxietyEntityResponse(
            anxiety.id,
            anxiety.description,
            anxiety.created,
            anxiety.chanceAssessments.map { mapChanceAssessment(it) },
            mapResolution(anxiety.resolution),
            anxiety.detail?.let { mapDetail(it) }
        )
    }

    private fun mapResolution(resolution: Resolution?): AnxietyResolutionResponse {
        return when (resolution?.fulfilled) {
            true -> AnxietyResolutionResponse(
                AnxietyResolutionType.FULFILLED,
                resolution.created
            )
            false -> AnxietyResolutionResponse(
                AnxietyResolutionType.UNFULFILLED,
                resolution.created
            )
            else -> AnxietyResolutionResponse(
                AnxietyResolutionType.UNRESOLVED,
                resolution?.created
            )
        }
    }

    private fun mapChanceAssessment(chance: ChanceAssessment): AnxietyChanceAssessmentResponse {
        return AnxietyChanceAssessmentResponse(
            chance.chance.level,
            chance.created
        )
    }

    private fun mapDetail(detail: AnxietyDetail): AnxietyDetailResponse {
        return AnxietyDetailResponse(
            detail.trigger,
            detail.bodyResponse,
            detail.anxietyResponse,
            detail.alternativeThoughts
        )
    }
}