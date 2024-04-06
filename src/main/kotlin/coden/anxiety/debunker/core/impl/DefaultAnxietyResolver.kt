package coden.anxiety.debunker.core.impl

import coden.anxiety.debunker.core.api.*
import coden.anxiety.debunker.core.api.persistance.Resolution
import coden.anxiety.debunker.core.api.persistance.ResolutionRespository
import org.apache.logging.log4j.kotlin.Logging

class DefaultAnxietyResolver(
    private val repository: ResolutionRespository
) : AnxietyResolver, Logging {
    override fun resolve(request: ResolveAnxietyRequest): Result<ResolveAnxietyResponse> {
        logger.info("Resolving anxiety ${request.anxietyId} -> fulfilled: ${request.fulfilled}...")

        val resolution = Resolution(request.anxietyId, request.fulfilled)
        return repository
            .insert(resolution)
            .map { ResolveAnxietyResponse(resolution.anxietyId, resolution.fulfilled, resolution.resolvedAt) }
            .logInteraction(logger, "Resolving anxiety ${request.anxietyId}")
    }

    override fun unresolve(request: UnresolveAnxietyRequest): Result<UnresolveAnxietyResponse> {
        logger.info("Unresolving anxiety ${request.anxietyId}...")

        return repository
            .delete(request.anxietyId)
            .map { UnresolveAnxietyResponse(request.anxietyId) }
            .logInteraction(logger, "Unresolving anxiety ${request.anxietyId}")
    }

    override fun update(request: UpdateResolutionRequest): Result<UpdateResolutionResponse> {
        logger.info("Updating resolution for ${request.anxietyId} -> fulfilled: ${request.fulfilled}...")

        val resolution = Resolution(request.anxietyId, request.fulfilled)
        return repository
            .update(resolution)
            .map { UpdateResolutionResponse(it.anxietyId, it.fulfilled, it.resolvedAt) }
            .logInteraction(logger, "Updating resolution for ${request.anxietyId}")
    }

    override fun clear(request: ClearResolutionsRequest): Result<ClearResolutionsResponse> {
        logger.info("Clearing resolutions...")

        return repository
            .clear()
            .map { ClearResolutionsResponse(it) }
            .logInteraction(logger, "Clearing resolutions")
    }
}