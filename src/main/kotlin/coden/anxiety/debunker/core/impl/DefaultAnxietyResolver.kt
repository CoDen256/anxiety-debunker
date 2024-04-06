package coden.anxiety.debunker.core.impl

import coden.anxiety.debunker.core.api.*
import coden.anxiety.debunker.core.persistance.AnxietyRepository
import coden.anxiety.debunker.core.persistance.Resolution
import coden.utils.logInteraction
import org.apache.logging.log4j.kotlin.Logging

class DefaultAnxietyResolver(
    private val repository: AnxietyRepository
) : AnxietyResolver, Logging {
    override fun resolve(request: ResolveAnxietyRequest): Result<ResolveAnxietyResponse> {
        logger.info("Resolving anxiety ${request.anxietyId} -> fulfilled: ${request.fulfilled}...")

        val resolution = Resolution(request.anxietyId, request.fulfilled)
        return repository
            .saveResolution(resolution)
            .map { ResolveAnxietyResponse(resolution.anxietyId, resolution.fulfilled, resolution.resolvedAt) }
            .logInteraction(logger, "Resolving anxiety ${request.anxietyId}")
    }

    override fun unresolve(request: UnresolveAnxietyRequest): Result<UnresolveAnxietyResponse> {
        logger.info("Unresolving anxiety ${request.anxietyId}...")

        return repository
            .deleteResolution(request.anxietyId)
            .map { UnresolveAnxietyResponse(request.anxietyId) }
            .logInteraction(logger, "Unresolving anxiety ${request.anxietyId}")
    }

    override fun update(request: UpdateResolutionRequest): Result<UpdateResolutionResponse> {
        logger.info("Updating resolution for ${request.anxietyId} -> fulfilled: ${request.fulfilled}...")

        val resolution = Resolution(request.anxietyId, request.fulfilled)
        return repository
            .updateResolution(resolution)
            .map { UpdateResolutionResponse(it.anxietyId, it.fulfilled, it.resolvedAt) }
            .logInteraction(logger, "Updating resolution for ${request.anxietyId}")
    }

    override fun clear(request: ClearResolutionsRequest): Result<ClearResolutionsResponse> {
        logger.info("Clearing resolutions...")

        return repository
            .clearResolutions()
            .map { ClearResolutionsResponse(it) }
            .logInteraction(logger, "Clearing resolutions")
    }
}