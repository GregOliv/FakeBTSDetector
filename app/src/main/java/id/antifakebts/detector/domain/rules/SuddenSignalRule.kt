package id.antifakebts.detector.domain.rules

import id.antifakebts.detector.data.model.CellObservation

class SuddenSignalRule(
    override val defaultWeight: Int = 30
) : CellularRule {
    override val id: String = "RULE_SUDDEN_SIGNAL_SPIKE"
    override val name: String = "Sinyal Sangat Kuat Mendadak"
    override val category: SignalCategory = SignalCategory.NETWORK_BEHAVIOR

    override suspend fun evaluate(
        current: CellObservation,
        history: List<CellObservation>,
        isUserMoving: Boolean,
        isRoaming: Boolean
    ): RuleEvaluation {
        val currentDbm = current.signalDbm ?: return RuleEvaluation(id, name, category, false, 0)

        // Weakening: Movement brings device close to legitimate towers
        if (isUserMoving) {
            return RuleEvaluation(
                id = id,
                name = name,
                category = category,
                isTriggered = false,
                scoreContribution = 0,
                weakeningReason = "Perubahan sinyal terjadi saat pengguna sedang bergerak"
            )
        }

        val previousSameLocation = history.take(5).filter { it.signalDbm != null }
        if (previousSameLocation.isEmpty()) {
            return RuleEvaluation(id, name, category, false, 0)
        }

        val avgPreviousDbm = previousSameLocation.mapNotNull { it.signalDbm }.average()
        val delta = currentDbm - avgPreviousDbm

        // Sudden surge: Jump of >= 25 dBm to high power while stationary
        if (delta >= SIGNAL_SURGE_DELTA_DBM && currentDbm >= HIGH_POWER_FLOOR_DBM) {
            return RuleEvaluation(
                id = id,
                name = name,
                category = category,
                isTriggered = true,
                scoreContribution = defaultWeight,
                primaryReason = "Sinyal melonjak drastis sebesar +${delta.toInt()} dBm menjadi $currentDbm dBm tanpa pergerakan lokasi"
            )
        }

        return RuleEvaluation(id, name, category, false, 0)
    }

    companion object {
        const val SIGNAL_SURGE_DELTA_DBM = 25
        const val HIGH_POWER_FLOOR_DBM = -70
    }
}
