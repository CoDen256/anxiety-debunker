package coden.anxiety.debunker.core.api

import java.time.Instant

interface AnxietyResolver {
    fun resolve(request: ResolveAnxietyRequest): Result<ResolveAnxietyResponse>
    fun unresolve(request: UnresolveAnxietyRequest): Result<UnresolveAnxietyResponse>
    fun update(request: UpdateResolutionRequest): Result<UpdateResolutionResponse>

    fun clear(request: ClearResolutionsRequest): Result<ClearResolutionsResponse>
}


sealed interface AnxietyResolverRequest

data class ResolveAnxietyRequest(
    val anxietyId: String,
    val fulfilled: Boolean
): AnxietyResolverRequest

data class UnresolveAnxietyRequest(
    val anxietyId: String,
): AnxietyResolverRequest

data class UpdateResolutionRequest(
    val anxietyId: String,
    val fulfilled: Boolean
): AnxietyResolverRequest

data object ClearResolutionsRequest: AnxietyResolverRequest

sealed interface AnxietyResolverResponse

data class ResolveAnxietyResponse(
    val anxietyId: String,
    val fulfilled: Boolean,
    val resolvedAt: Instant
): AnxietyResolverResponse

data class UnresolveAnxietyResponse(
    val anxietyId: String,
): AnxietyResolverResponse

data class UpdateResolutionResponse(
    val anxietyId: String,
    val fulfilled: Boolean,
    val resolvedAt: Instant
): AnxietyResolverRequest

data class ClearResolutionsResponse(
    val count: Long
): AnxietyResolverResponse