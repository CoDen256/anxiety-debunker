package io.github.coden256.anxiety.debunker.core.persistance

import java.time.Instant

data class  ChanceAssessment(
    val anxietyId: String,
    val chance: Chance,
    val id: String,
    val created: Instant = Instant.now(),
)

@JvmInline
value class Chance(val level : Int){
    init {
        if (level > HIGHEST_LEVEL){
            throw IllegalArgumentException("Risk level must not be larger than $HIGHEST")
        }
        if (level < LOWEST_LEVEL){
            throw IllegalArgumentException("Risk level must not be smaller than $LOWEST")
        }
    }
    companion object {
        const val HIGHEST_LEVEL = 100
        const val LOWEST_LEVEL = 0
        val HIGHEST: Chance = Chance(HIGHEST_LEVEL)
        val LOWEST: Chance = Chance(LOWEST_LEVEL)
        fun Number.chance() = Chance(this.toInt())
    }

}