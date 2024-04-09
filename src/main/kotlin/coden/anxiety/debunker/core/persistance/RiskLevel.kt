package coden.anxiety.debunker.core.persistance

@JvmInline
value class RiskLevel(val level : Int){
    init {
        if (level > MAX.level){
            throw IllegalArgumentException("Risk level must not be larger than $MAX")
        }
        if (level < MIN.level){
            throw IllegalArgumentException("Risk level must not be smaller than $MIN")
        }
    }
    companion object {
        val MAX: RiskLevel = RiskLevel(10)
        val MIN: RiskLevel = RiskLevel(0)
        fun Number.asRisk() = RiskLevel(this.toInt())
    }

}