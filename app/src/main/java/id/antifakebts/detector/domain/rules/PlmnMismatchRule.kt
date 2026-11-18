package id.antifakebts.detector.domain.rules

import id.antifakebts.detector.data.model.CellObservation

class PlmnMismatchRule(
    private val simPlmnProvider: () -> String?,
    override val defaultWeight: Int = 35
) : CellularRule {
    override val id: String = "RULE_PLMN_MISMATCH"
    override val name: String = "Diskrepansi Identitas Operator (PLMN)"
    override val category: SignalCategory = SignalCategory.CELL_IDENTITY

    override suspend fun evaluate(
        current: CellObservation,
        history: List<CellObservation>,
        isUserMoving: Boolean,
        isRoaming: Boolean
    ): RuleEvaluation {
        val cellPlmn = current.plmn ?: return RuleEvaluation(id, name, category, false, 0)
        val simPlmn = simPlmnProvider() ?: return RuleEvaluation(id, name, category, false, 0)

        // Roaming is legitimate normal operation for PLMN differences
        if (isRoaming) {
            return RuleEvaluation(
                id = id,
                name = name,
                category = category,
                isTriggered = false,
                scoreContribution = 0,
                weakeningReason = "Perbedaan operator PLMN wajar karena perangkat dalam status roaming"
            )
        }

        if (cellPlmn != simPlmn) {
            // Indonesia national multi-brand roaming allowances (e.g. Indosat 51001 and Tri 51089)
            val isKnownConsortium = (cellPlmn.startsWith("510") && simPlmn.startsWith("510")) &&
                    ((cellPlmn in listOf("51001", "51089") && simPlmn in listOf("51001", "51089")))

            if (isKnownConsortium) {
                return RuleEvaluation(
                    id = id,
                    name = name,
                    category = category,
                    isTriggered = false,
                    scoreContribution = 5,
                    weakeningReason = "Perbedaan PLMN dalam konsorsium operator gabungan yang sah (misal Indosat & Tri)"
                )
            }

            return RuleEvaluation(
                id = id,
                name = name,
                category = category,
                isTriggered = true,
                scoreContribution = defaultWeight,
                primaryReason = "Sel menyiarkan PLMN ($cellPlmn) yang berbeda dari kartu SIM aktif ($simPlmn) tanpa status roaming"
            )
        }

        return RuleEvaluation(id, name, category, false, 0)
    }
}
