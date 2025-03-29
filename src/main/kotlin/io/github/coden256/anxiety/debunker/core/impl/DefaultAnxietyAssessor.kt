package io.github.coden256.anxiety.debunker.core.impl

import io.github.coden256.anxiety.debunker.core.api.*
import io.github.coden256.anxiety.debunker.core.persistance.AnxietyRepository
import io.github.coden256.anxiety.debunker.core.persistance.Chance.Companion.chance
import io.github.coden256.anxiety.debunker.core.persistance.ChanceAssessment
import io.github.coden256.utils.flatMap
import io.github.coden256.utils.log
import org.apache.logging.log4j.kotlin.Logging

class DefaultAnxietyAssessor(private val repository: AnxietyRepository): AnxietyAssessor, Logging {

    override fun assess(request: NewChanceAssessmentRequest): Result<NewChanceAssessmentResponse> {
        logger.info("Adding new chance of ${request.chance} units for ${request.anxietyId}...")

        return repository.getNextChanceAssessmentId(request.anxietyId)
            .map { id -> ChanceAssessment(request.anxietyId, request.chance.level.chance(), id) }
            .flatMap { repository.saveChanceAssessment(it) }
            .map { NewChanceAssessmentResponse(it.id, it.anxietyId, it.chance.level) }
            .log(logger){"Added chance (${it.id}) of ${it.chance}] for ${it.anxietyId}"}
    }

    override fun remove(request: DeleteChanceAssessmentRequest): Result<DeleteChanceAssessmentResponse> {
        logger.info("Deleting chance ${request.id}...")

        return repository
            .deleteChanceAssessmentById(request.id)
            .map { DeleteChanceAssessmentResponse(it.id) }
            .log(logger){"Deleted chance ${it.id}"}
    }

    override fun clear(request: ClearChanceAssessmentsRequest): Result<ClearChanceAssessmentsResponse> {
        logger.info("Clearing chances...")

        return repository
            .clearChanceAssessments()
            .map { ClearChanceAssessmentsResponse(it) }
            .log(logger){"Cleared chances"}
    }
}