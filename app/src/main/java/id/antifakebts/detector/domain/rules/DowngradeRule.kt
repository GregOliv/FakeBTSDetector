package id.antifakebts.detector.domain.rules

import id.antifakebts.detector.data.model.CellObservation

class DowngradeRule(
    override val defaultWeight: Int = 40
) : CellularRule {
    override val id: String = "RULE_FORCED_2G_DOWNGRADE"
    override val name: String = "Penurunan Paksa ke 2G"
    override val category: SignalCategory = SignalCategory.NETWORK_BEHAVIOR

    override suspend fun evaluate(
        current: CellObservation,
        history: List<CellObservation>,
        isUserMoving: Boolean,
        isRoaming: Boolean
    ): RuleEvaluation {
        if (!current.networkType.is2G()) {
            return RuleEvaluation(id, name, category, false, 0)
        }

        val recentModernCells = history.take(5).filter { it.networkType.isModern() }
        if (recentModernCells.isEmpty()) {
            return RuleEvaluation(
                id = id,
                name = name,
                category = category,
                isTriggered = false,
                scoreContribution = 0,
                weakeningReason = "Perangkat memang telah berada di jaringan 2G secara konsisten"
            )
        }

        val lastModernCell = recentModernCells.first()
        val prevRsrp = lastModernCell.signalRsrp ?: lastModernCell.signalDbm ?: -100

        // If previous signal was weak, 2G fallback is normal physical behavior
        if (prevRsrp < COVERAGE_EDGE_THRESHOLD_DBM || isUserMoving) {
            return RuleEvaluation(
                id = id,
                name = name,
                category = category,
                isTriggered = false,
                scoreContribution = 5,
                weakeningReason = "Transisi 2G wajar: sinyal 4G/5G sebelumnya lemah ($prevRsrp dBm) atau perangkat bergerak"
            )
        }

        return RuleEvaluation(
            id = id,
            name = name,
            category = category,
            isTriggered = true,
            scoreContribution = defaultWeight,
            primaryReason = "Penurunan mendadak ke 2G padahal sinyal 4G/5G sebelumnya sangat kuat ($prevRsrp dBm)"
        )
    }

    companion object {
        const val COVERAGE_EDGE_THRESHOLD_DBM = -108
    }
}
