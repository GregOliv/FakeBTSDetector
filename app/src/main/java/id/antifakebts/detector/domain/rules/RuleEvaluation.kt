package id.antifakebts.detector.domain.rules

import kotlinx.serialization.Serializable

@Serializable
data class RuleEvaluation(
    val ruleId: String,
    val ruleName: String,
    val category: SignalCategory,
    val isTriggered: Boolean,
    val scoreContribution: Int,
    val primaryReason: String? = null,
    val weakeningReason: String? = null
)
