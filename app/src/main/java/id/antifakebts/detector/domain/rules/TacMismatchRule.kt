package id.antifakebts.detector.domain.rules

import id.antifakebts.detector.data.model.CellObservation
import id.antifakebts.detector.data.repository.BaselineRepository

class TacMismatchRule(
    private val baselineRepository: BaselineRepository,
    override val defaultWeight: Int = 35
) : CellularRule {
    override val id: String = "RULE_TAC_LAC_INCONSISTENCY"
    override val name: String = "Inkonsistensi Nilai TAC / LAC"
    override val category: SignalCategory = SignalCategory.CELL_IDENTITY

    override suspend fun evaluate(
        current: CellObservation,
        history: List<CellObservation>,
        isUserMoving: Boolean,
        isRoaming: Boolean
    ): RuleEvaluation {
        val currentCid = current.cellId ?: return RuleEvaluation(id, name, category, false, 0)
        val currentTac = current.lacTac ?: return RuleEvaluation(id, name, category, false, 0)

        // Check 1: In baseline, does the same cell ID have a different TAC?
        val historicalBaselines = baselineRepository.getBaselineForCell(currentCid)
        val conflictInBaseline = historicalBaselines.any { it.lacTac != currentTac }
        if (conflictInBaseline) {
            return RuleEvaluation(
                id = id,
                name = name,
                category = category,
                isTriggered = true,
                scoreContribution = defaultWeight,
                primaryReason = "Cell ID ($currentCid) terdeteksi memiliki TAC berbeda ($currentTac) dibanding rekaman baseline"
            )
        }

        // Check 2: Rapid TAC switching while stationary
        if (!isUserMoving) {
            val distinctTacs = history.take(6).mapNotNull { it.lacTac }.distinct()
            if (distinctTacs.size >= 3) {
                return RuleEvaluation(
                    id = id,
                    name = name,
                    category = category,
                    isTriggered = true,
                    scoreContribution = defaultWeight,
                    primaryReason = "Nilai TAC/LAC berubah-ubah (${distinctTacs.joinToString()}) tanpa adanya pergerakan fisik"
                )
            }
        }

        return RuleEvaluation(id, name, category, false, 0)
    }
}
