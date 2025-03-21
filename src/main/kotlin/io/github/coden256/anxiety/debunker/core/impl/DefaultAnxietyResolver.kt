package io.github.coden256.anxiety.debunker.core.impl

import io.github.coden256.anxiety.debunker.core.api.*
import io.github.coden256.anxiety.debunker.core.persistance.AnxietyRepository
import io.github.coden256.anxiety.debunker.core.persistance.Resolution
import io.github.coden256.utils.logResult
import org.apache.logging.log4j.kotlin.Logging

class DefaultAnxietyResolver(
    private val repository: AnxietyRepository
) : AnxietyResolver, Logging {
    override fun resolve(request: ResolveAnxietyRequest): Result<ResolveAnxietyResponse> {
        logger.info("Resolving anxiety ${request.anxietyId} -> fulfilled: ${request.fulfilled}...")

        return repository
            .saveResolution(Resolution(request.anxietyId, request.fulfilled))
            .map { ResolveAnxietyResponse(it.anxietyId, it.fulfilled, it.created) }
            .logResult(logger){ "Resolved ${it.anxietyId}"}
    }

    override fun unresolve(request: UnresolveAnxietyRequest): Result<UnresolveAnxietyResponse> {
        logger.info("Unresolving anxiety ${request.anxietyId}...")

        return repository
            .deleteResolutionByAnxietyId(request.anxietyId)
            .map { UnresolveAnxietyResponse(it.anxietyId) }
            .logResult(logger){ "Unresolved ${it.anxietyId}"}
    }

    override fun update(request: UpdateResolutionRequest): Result<UpdateResolutionResponse> {
        logger.info("Updating resolution for ${request.anxietyId} -> fulfilled: ${request.fulfilled}...")

        return repository
            .updateResolution(Resolution(request.anxietyId, request.fulfilled))
            .map { UpdateResolutionResponse(it.anxietyId, it.fulfilled, it.created) }
            .logResult(logger){ "Updated ${request.anxietyId}"}
    }

    override fun clear(request: ClearResolutionsRequest): Result<ClearResolutionsResponse> {
        logger.info("Clearing resolutions...")

        return repository
            .clearResolutions()
            .map { ClearResolutionsResponse(it) }
            .logResult(logger){ "Cleared resolutions"}
    }
}