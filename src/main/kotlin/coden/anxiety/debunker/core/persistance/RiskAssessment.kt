package coden.anxiety.debunker.core.persistance

import org.apache.commons.lang3.RandomStringUtils
import java.time.Instant

data class RiskAssessment(
    val anxietyId: String,
    val risk: RiskLevel,
    val assessed: Instant = Instant.now(),
    val id: String = RandomStringUtils.randomAlphabetic(    5)
)