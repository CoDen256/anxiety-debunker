package coden.anxiety.debunker.core.impl

import coden.anxiety.debunker.core.api.*
import coden.anxiety.debunker.core.persistance.Anxiety
import coden.anxiety.debunker.core.persistance.AnxietyRepository
import coden.utils.logInteraction
import org.apache.logging.log4j.kotlin.Logging

class DefaultAnxietyHolder(
    private val repository: AnxietyRepository
) : AnxietyHolder, Logging {
    override fun add(request: NewAnxietyRequest): Result<NewAnxietyResponse> {
        logger.info("Adding new anxiety ${request.description.take(15)}[...]")

        val anxiety = Anxiety(request.description)
        return repository
            .saveAnxiety(anxiety)
            .map { NewAnxietyResponse(anxiety.id, anxiety.description, anxiety.created) }
            .logInteraction(logger, "Adding new anxiety(${anxiety.id})")
    }

    override fun delete(request: DeleteAnxietyRequest): Result<DeleteAnxietyResponse> {
        logger.info("Removing anxiety(${request.id})...")

        return repository
            .deleteAnxiety(request.id)
            .map { DeleteAnxietyResponse(request.id) }
            .logInteraction(logger, "Removing anxiety(${request.id})")
    }

    override fun update(request: UpdateAnxietyRequest): Result<UpdateAnxietyResponse> {
        logger.info("Updating anxiety(${request.id}) -> ${request.description}...")

        return repository
            .updateAnxiety(request.id, request.description)
            .map { UpdateAnxietyResponse(it.id, it.description, it.created) }
            .logInteraction(logger, "Updating anxiety(${request.id})")
    }

    override fun clear(request: ClearAnxietiesRequest): Result<ClearAnxietiesResponse> {
        logger.info("Clearing anxieties...")

        return repository
            .clearAnxieties()
            .map { ClearAnxietiesResponse(it) }
            .logInteraction(logger, "Clearing anxieties")
    }
}