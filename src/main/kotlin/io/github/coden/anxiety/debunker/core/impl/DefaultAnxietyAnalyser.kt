package io.github.coden.anxiety.debunker.core.impl

import io.github.coden.anxiety.debunker.core.api.*
import io.github.coden.anxiety.debunker.core.persistance.Anxiety
import io.github.coden.anxiety.debunker.core.persistance.AnxietyRepository
import io.github.coden.anxiety.debunker.core.persistance.ChanceAssessment
import io.github.coden.anxiety.debunker.core.persistance.Resolution
import io.github.coden.utils.logResult
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
            .logResult(logger){ "Got anxiety ${it.id}..."}
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
            .logResult(logger){ "Got all anxieties"}
    }

    private fun mapAnxietyToEntityResponse(anxiety: Anxiety): AnxietyEntityResponse {
        return AnxietyEntityResponse(
            anxiety.id,
            anxiety.description,
            anxiety.created,
            anxiety.chanceAssessments.map { mapChanceAssessment(it) },
            mapResolution(anxiety.resolution),
        )
    }

    private fun mapResolution(resolution: Resolution?): AnxietyResolutionResponse {
        return when (resolution?.fulfilled) {
            true -> AnxietyResolutionResponse(AnxietyResolutionType.FULFILLED, resolution.created)
            false -> AnxietyResolutionResponse(AnxietyResolutionType.UNFULFILLED, resolution.created)
            else -> AnxietyResolutionResponse(AnxietyResolutionType.UNRESOLVED,  resolution?.created)
        }
    }

    private fun mapChanceAssessment(chance: ChanceAssessment): AnxietyChanceAssessmentResponse {
        return AnxietyChanceAssessmentResponse(chance.chance.level, chance.created)
    }
}