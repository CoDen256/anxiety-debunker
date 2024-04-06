package coden.anxiety.debunker.core.impl

import coden.anxiety.debunker.core.api.*
import coden.anxiety.debunker.core.api.persistance.*
import org.apache.logging.log4j.kotlin.Logging

class DefaultAnxietyAnalyser
    (
    private val anxietyRepository: AnxietyRepository,
    private val resolutionRespository: ResolutionRespository
) : AnxietyAnalyser, Logging {
    override fun anxiety(request: AnxietyRequest): Result<AnxietyEntityResponse> {
        logger.info("Requesting anxiety ${request.id}...")

        return anxietyRepository
            .get(request.id)
            .flatMap { mapAnxietyToEntityResponse(it) }
            .logInteraction(logger, "Requesting anxiety ${request.id}...")
    }

    private fun mapAnxietyToEntityResponse(anxiety: Anxiety): Result<AnxietyEntityResponse> {
        return getResolution(anxiety.id)
            .map { resolution -> resolution to anxiety }
            .map { AnxietyEntityResponse(it.second.id, it.second.description, it.first, it.second.created) }
    }

    private fun getResolution(id: String): Result<AnxietyResolution> {
        return resolutionRespository
            .get(id)
            .map { if (it.fulfilled) AnxietyResolution.FULFILLED else AnxietyResolution.UNFULFILLED }
            .recover<NoResolutionException, AnxietyResolution> { AnxietyResolution.UNRESOLVED }
    }

    override fun anxieties(request: ListAnxietyRequest): Result<AnxietyListResponse> {
        logger.info("Requesting all anxieties...")

        return anxietyRepository
            .list()
            .flatMap {
                it.map { mapAnxietyToEntityResponse(it) }
                AnxietyListResponse()
            }
            .logInteraction(logger, "Requesting all anxieties")
    }

    override fun resolution(request: ResolutionRequest): Result<ResolutionEntityResponse> {
        logger.info("Requesting anxiety ${request.id}...")

        val anxiety = Anxiety(request.description)
        return repository
            .insert(anxiety)
            .map { NewAnxietyResponse(anxiety.id, anxiety.description, anxiety.created) }
            .logInteraction(logger, "Adding new anxiety(${anxiety.id})")
    }

    override fun resolutions(request: ListResolutionRequest): Result<ResolutionListResponse> {
        logger.info("Requesting anxiety ${request.id}...")

        val anxiety = Anxiety(request.description)
        return repository
            .insert(anxiety)
            .map { NewAnxietyResponse(anxiety.id, anxiety.description, anxiety.created) }
            .logInteraction(logger, "Adding new anxiety(${anxiety.id})")
    }
}