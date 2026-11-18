package id.antifakebts.detector.domain.rules

import id.antifakebts.detector.data.model.CellObservation

class NeighborListRule(
    override val defaultWeight: Int = 25
) : CellularRule {
    override val id: String = "RULE_EMPTY_NEIGHBOR_LIST"
    override val name: String = "Sel Terisolasi Tanpa Sel Tetangga"
    override val category: SignalCategory = SignalCategory.TOPOLOGY

    override suspend fun evaluate(
        current: CellObservation,
        history: List<CellObservation>,
        isUserMoving: Boolean,
        isRoaming: Boolean
    ): RuleEvaluation {
        if (current.neighborCount > 0) {
            return RuleEvaluation(id, name, category, false, 0)
        }

        val currentDbm = current.signalDbm ?: -100

        // Weakening: In weak rural coverage, 0 neighbors is common and normal
        if (currentDbm < RURAL_WEAK_SIGNAL_DBM) {
            return RuleEvaluation(
                id = id,
                name = name,
                category = category,
                isTriggered = false,
                scoreContribution = 0,
                weakeningReason = "Ketiadaan sel tetangga wajar pada area sinyal lemah ($currentDbm dBm)"
            )
        }

        // Check if previous history in this area normally saw neighbors
        val historyWithNeighbors = history.take(10).count { it.neighborCount > 0 }
        if (historyWithNeighbors >= 3 && currentDbm >= STRONG_ISOLATED_SIGNAL_DBM) {
            return RuleEvaluation(
                id = id,
                name = name,
                category = category,
                isTriggered = true,
                scoreContribution = defaultWeight,
                primaryReason = "Sel aktif dengan sinyal kuat ($currentDbm dBm) tidak menyiarkan daftar sel tetangga (neighbor list)"
            )
        }

        return RuleEvaluation(id, name, category, false, 0)
    }

    companion object {
        const val RURAL_WEAK_SIGNAL_DBM = -105
        const val STRONG_ISOLATED_SIGNAL_DBM = -88
    }
}
