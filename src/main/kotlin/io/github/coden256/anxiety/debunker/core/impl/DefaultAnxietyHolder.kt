package io.github.coden256.anxiety.debunker.core.impl

import io.github.coden256.anxiety.debunker.core.api.*
import io.github.coden256.anxiety.debunker.core.persistance.Anxiety
import io.github.coden256.anxiety.debunker.core.persistance.AnxietyRepository
import io.github.coden256.utils.flatMap
import io.github.coden256.utils.log
import org.apache.logging.log4j.kotlin.Logging

class DefaultAnxietyHolder(
    private val repository: AnxietyRepository
) : AnxietyHolder, Logging {
    override fun add(request: NewAnxietyRequest): Result<NewAnxietyResponse> {
        logger.info("Adding new anxiety ${request.description.take(15)}[...]")

        return repository.getNextAnxietyId()
            .flatMap { id -> repository.saveAnxiety(Anxiety(request.description, id)) }
            .map { NewAnxietyResponse(it.id, it.description, it.created) }
            .log(logger){ "Added new anxiety(${it.id})"}
    }

    override fun delete(request: DeleteAnxietyRequest): Result<DeleteAnxietyResponse> {
        logger.info("Removing anxiety(${request.id})...")

        return repository.deleteAnxietyById(request.id)
            .map { DeleteAnxietyResponse(it.id) }
            .log(logger){ "Removed anxiety(${it.id})"}
    }

    override fun update(request: UpdateAnxietyRequest): Result<UpdateAnxietyResponse> {
        logger.info("Updating anxiety(${request.id}) -> ${request.description}...")

        return repository
            .getAnxietyById(request.id)
            .flatMap { repository.updateAnxiety(it.copy(description=request.description)) }
            .map { UpdateAnxietyResponse(it.id, it.description, it.created) }
            .log(logger){ "Updated anxiety(${it.id})"}
    }

    override fun clear(request: ClearAnxietiesRequest): Result<ClearAnxietiesResponse> {
        logger.info("Clearing anxieties...")

        return repository
            .clearAnxieties()
            .map { ClearAnxietiesResponse(it) }
            .log(logger){ "Cleared anxieties"}
    }
}