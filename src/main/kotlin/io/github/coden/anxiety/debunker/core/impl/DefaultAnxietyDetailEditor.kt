package io.github.coden.anxiety.debunker.core.impl

import io.github.coden.anxiety.debunker.core.api.*
import io.github.coden.anxiety.debunker.core.persistance.AnxietyDetail
import io.github.coden.anxiety.debunker.core.persistance.AnxietyRepository
import io.github.coden.utils.logResult
import org.apache.logging.log4j.kotlin.Logging

class DefaultAnxietyDetailEditor(
    private val repository: AnxietyRepository
) : AnxietyDetailEditor, Logging {

    override fun add(request: NewDetailRequest): Result<NewDetailResponse> {
        logger.info("Adding details for anxiety ${request.anxietyId}...")

        return repository
            .saveDetail(AnxietyDetail(request.anxietyId, request.trigger, request.bodyResponse, request.anxietyResponse, request.alternativeThoughts))
            .map { NewDetailResponse(it.anxietyId, it.trigger, it.bodyResponse, it.anxietyResponse, request.alternativeThoughts) }
            .logResult(logger){ "Added details to ${it.anxietyId}"}
    }

    override fun update(request: UpdateDetailRequest): Result<UpdateDetailResponse> {
        logger.info("Updating anxiety details(${request.anxietyId})")

        return repository
            .updateDetail(AnxietyDetail(request.anxietyId, request.trigger, request.bodyResponse, request.anxietyResponse, request.alternativeThoughts))
            .map { UpdateDetailResponse(it.anxietyId, it.trigger, it.bodyResponse, it.anxietyResponse, it.alternativeThoughts) }
            .logResult(logger){ "Updated anxiety details(${it.anxietyId})"}
    }

    override fun clear(request: ClearDetailsRequest): Result<ClearDetailsResponse> {
        logger.info("Clearing resolutions...")

        return repository
            .clearDetails()
            .map { ClearDetailsResponse(it) }
            .logResult(logger){ "Cleared anxiety details"}
    }

    override fun remove(request: DeleteDetailRequest): Result<DeleteDetailResponse> {
        logger.info("Removing anxiety details for ${request.anxietyId}...")

        return repository
            .deleteDetailByAnxietyId(request.anxietyId)
            .map { DeleteDetailResponse(it.anxietyId) }
            .logResult(logger){ "Deleted details for ${it.anxietyId}"}
    }


}