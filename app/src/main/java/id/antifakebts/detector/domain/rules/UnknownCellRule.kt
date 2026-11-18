package id.antifakebts.detector.domain.rules

import id.antifakebts.detector.data.model.CellObservation
import id.antifakebts.detector.data.repository.BaselineRepository

class UnknownCellRule(
    private val baselineRepository: BaselineRepository,
    override val defaultWeight: Int = 30
) : CellularRule {
    override val id: String = "RULE_UNKNOWN_CELL_ID"
    override val name: String = "Identitas Sel Baru / Tidak Dikenal"
    override val category: SignalCategory = SignalCategory.CELL_IDENTITY

    override suspend fun evaluate(
        current: CellObservation,
        history: List<CellObservation>,
        isUserMoving: Boolean,
        isRoaming: Boolean
    ): RuleEvaluation {
        val cellId = current.cellId ?: return RuleEvaluation(id, name, category, false, 0)

        if (baselineRepository.isCellWhitelisted(cellId)) {
            return RuleEvaluation(
                id = id,
                name = name,
                category = category,
                isTriggered = false,
                scoreContribution = 0,
                weakeningReason = "Sel ini telah ditandai aman oleh pengguna (whitelist)"
            )
        }

        val isKnownLocally = baselineRepository.isCellKnown(cellId)
        if (isKnownLocally) {
            return RuleEvaluation(
                id = id,
                name = name,
                category = category,
                isTriggered = false,
                scoreContribution = 0,
                weakeningReason = "Sel telah terverifikasi dalam baseline lokal"
            )
        }

        // Weakening: In dynamic cellular environments, new cells with healthy neighbors are normal
        if (current.neighborCount >= 2) {
            return RuleEvaluation(
                id = id,
                name = name,
                category = category,
                isTriggered = false,
                scoreContribution = 5,
                weakeningReason = "Sel belum tercatat di baseline, namun memiliki ${current.neighborCount} sel tetangga wajar"
            )
        }

        return RuleEvaluation(
            id = id,
            name = name,
            category = category,
            isTriggered = true,
            scoreContribution = defaultWeight,
            primaryReason = "Sel ID ($cellId) belum pernah terlihat di lokasi ini dan tidak memiliki daftar sel tetangga"
        )
    }
}
