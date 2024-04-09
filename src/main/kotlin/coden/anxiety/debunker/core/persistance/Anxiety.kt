package coden.anxiety.debunker.core.persistance

import org.apache.commons.lang3.RandomStringUtils
import java.time.Instant

data class Anxiety(
    val id: String = RandomStringUtils.randomNumeric(8),
    val description: String,
    val created: Instant = Instant.now(),
    val resolution: Resolution?,
    val riskAssessments: List<RiskAssessment>
)