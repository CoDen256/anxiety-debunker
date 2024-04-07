package coden.anxiety.debunker.core.impl

import coden.anxiety.debunker.core.api.*
import coden.anxiety.debunker.core.persistance.AnxietyRepository
import coden.anxiety.debunker.core.persistance.AnxietyWithResolution
import coden.anxiety.debunker.core.persistance.Resolution
import coden.utils.logInteraction
import org.apache.logging.log4j.kotlin.Logging

class DefaultAnxietyAnalyser
    (
    private val anxietyRepository: AnxietyRepository,
) : AnxietyAnalyser, Logging {
    override fun anxiety(request: AnxietyRequest): Result<AnxietyEntityResponse> {
        logger.info("Requesting anxiety ${request.id}...")

        return anxietyRepository
            .anxiety(request.id)
            .map { mapAnxietyToEntityResponse(it) }
            .logInteraction(logger, "Requesting anxiety ${request.id}...")
    }

    override fun anxieties(request: ListAnxietiesRequest): Result<AnxietyListResponse> {
        logger.info("Requesting all anxieties...")

        return anxietyRepository
            .anxieties()
            .map { anxieties -> anxieties
                .map { mapAnxietyToEntityResponse(it) }
                .filter { request.filter.accept(it.resolution) }
            }
            .map { AnxietyListResponse(it) }
            .logInteraction(logger, "Requesting all anxieties")
    }

    private fun mapAnxietyToEntityResponse(anxiety: AnxietyWithResolution): AnxietyEntityResponse {
        return AnxietyEntityResponse(
            anxiety.id,
            anxiety.description,
            anxiety.created,
            mapResolution(anxiety.resolution),
            anxiety.resolution?.resolvedAt
        )
    }

    private fun mapResolution(resolution: Resolution?): AnxietyEntityResolution {
        return when (resolution?.fulfilled) {
            true -> AnxietyEntityResolution.FULFILLED
            false -> AnxietyEntityResolution.UNFULFILLED
            else -> AnxietyEntityResolution.UNRESOLVED
        }
    }
}