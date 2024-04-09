package coden.anxiety.debunker.core.persistance

@JvmInline
value class RiskLevel(val level : Int){
    init {
        if (level > MAX_LEVEL){
            throw IllegalArgumentException("Risk level must not be larger than $MAX")
        }
        if (level < MIN_LEVEL){
            throw IllegalArgumentException("Risk level must not be smaller than $MIN")
        }
    }
    companion object {
        const val MAX_LEVEL = 10
        const val MIN_LEVEL = 0
        val MAX: RiskLevel = RiskLevel(MAX_LEVEL)
        val MIN: RiskLevel = RiskLevel(MIN_LEVEL)
        fun Number.asRisk() = RiskLevel(this.toInt())
    }

}