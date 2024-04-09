package coden.anxiety.debunker.core.impl

import coden.anxiety.debunker.core.api.*
import coden.anxiety.debunker.core.persistance.AnxietyRepository
import coden.anxiety.debunker.core.persistance.RiskAssessment
import coden.utils.logInteraction
import org.apache.logging.log4j.kotlin.Logging

class DefaultAnxietyAssessor(private val repository: AnxietyRepository): AnxietyAssessor, Logging {

    override fun add(request: NewRiskRequest): Result<NewRiskResponse> {
        logger.info("Adding new risk of ${request.level} units for ${request.anxietyId}...")

        val risk = RiskAssessment(request.anxietyId, request.level)
        return repository
            .saveRiskAssessment(risk)
            .map { NewRiskResponse(risk.id, risk.anxietyId, risk.risk) }
            .logInteraction(logger, "Adding new risk of ${request.level}] for ${request.anxietyId}")
    }

    override fun remove(request: DeleteRiskRequest): Result<DeleteRiskResponse> {
        logger.info("Deleting risk ${request.id}...")

        return repository
            .deleteRiskAssessment(request.id)
            .map { DeleteRiskResponse(request.id) }
            .logInteraction(logger, "Deleting risk ${request.id}")
    }

    override fun clear(request: ClearRisksRequest): Result<ClearRisksResponse> {
        logger.info("Clearing risks...")

        return repository
            .clearRiskAssessments()
            .map { ClearRisksResponse(it) }
            .logInteraction(logger, "Clearing risks")
    }
}