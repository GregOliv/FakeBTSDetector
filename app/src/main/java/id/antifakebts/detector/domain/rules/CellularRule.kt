package id.antifakebts.detector.domain.rules

import id.antifakebts.detector.data.model.CellObservation

interface CellularRule {
    val id: String
    val name: String
    val category: SignalCategory
    val defaultWeight: Int

    suspend fun evaluate(
        current: CellObservation,
        history: List<CellObservation>,
        isUserMoving: Boolean,
        isRoaming: Boolean
    ): RuleEvaluation
}
