package coden.anxiety.debunker.core

import java.time.Instant

interface AnxietyResolver {
    fun resolve(request: ResolveRequest): Result<ResolveResponse>
    fun unresolve(request: UnresolveRequest): Result<UnresolveResponse>
    fun update(request: UpdateRequest): Result<UpdateResponse>

    fun resolution(request: ResolutionRequest): Result<ResolutionResponse>
    fun resolutions(request: ResolutionListRequest): Result<ResolutionListResponse>

    fun clear(request: ClearResolutionsRequest): Result<ClearResolutionsResponse>
}


sealed interface AnxietyResolverRequest

data class ResolveRequest(
    val anxietyId: Int,
    val fulfilled: Boolean
): AnxietyResolverRequest

data class UnresolveRequest(
    val anxietyId: Int,
): AnxietyResolverRequest

data class UpdateRequest(
    val anxietyId: Int,
    val fulfilled: Boolean
): AnxietyResolverRequest

data class ResolutionRequest(
    val anxietyId: Int,
): AnxietyResolverRequest

data object ResolutionListRequest : AnxietyResolverRequest
data object ClearResolutionsRequest: AnxietyResolverRequest

sealed interface AnxietyResolverResponse

data class ResolveResponse(
    val anxietyId: Int,
    val fulfilled: Boolean,
    val resolvedAt: Instant
): AnxietyResolverResponse

data class UnresolveResponse(
    val anxietyId: Int,
): AnxietyResolverResponse

data class UpdateResponse(
    val anxietyId: Int,
    val fulfilled: Boolean,
    val resolvedAt: Instant
): AnxietyResolverRequest

data class ResolutionResponse(
    val resolvedAt: Instant,
    val anxietyId: Int,
    val fulfilled: Boolean
): AnxietyResolverResponse

data class ResolutionListResponse(
    val resolutions: List<ResolutionResponse>
): AnxietyResolverResponse

data class ClearResolutionsResponse(
    val count: Long
): AnxietyResolverResponse