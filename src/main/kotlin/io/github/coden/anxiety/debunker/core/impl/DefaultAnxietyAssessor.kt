package io.github.coden.anxiety.debunker.core.impl

import io.github.coden.anxiety.debunker.core.api.*
import io.github.coden.anxiety.debunker.core.persistance.AnxietyRepository
import io.github.coden.anxiety.debunker.core.persistance.Chance.Companion.chance
import io.github.coden.anxiety.debunker.core.persistance.ChanceAssessment
import io.github.coden.utils.flatMap
import io.github.coden.utils.logInteraction
import org.apache.logging.log4j.kotlin.Logging

class DefaultAnxietyAssessor(private val repository: AnxietyRepository): AnxietyAssessor, Logging {

    override fun assess(request: NewChanceAssessmentRequest): Result<NewChanceAssessmentResponse> {
        logger.info("Adding new chance of ${request.chance} units for ${request.anxietyId}...")

        return repository.getNextChanceAssessmentId(request.anxietyId)
            .map { id -> ChanceAssessment(request.anxietyId, request.chance.level.chance(), id) }
            .flatMap { repository.saveChanceAssessment(it) }
            .map { NewChanceAssessmentResponse(it.id, it.anxietyId, it.chance.level) }
            .logInteraction(logger){"Added chance (${it.id}) of ${it.chance}] for ${it.anxietyId}"}
    }

    override fun remove(request: DeleteChanceAssessmentRequest): Result<DeleteChanceAssessmentResponse> {
        logger.info("Deleting chance ${request.id}...")

        return repository
            .deleteChanceAssessmentById(request.id)
            .map { DeleteChanceAssessmentResponse(it.id) }
            .logInteraction(logger){"Deleted chance ${it.id}"}
    }

    override fun clear(request: ClearChanceAssessmentsRequest): Result<ClearChanceAssessmentsResponse> {
        logger.info("Clearing chances...")

        return repository
            .clearChanceAssessments()
            .map { ClearChanceAssessmentsResponse(it) }
            .logInteraction(logger){"Cleared chances"}
    }
}