package id.antifakebts.detector.domain.rules

import kotlinx.serialization.Serializable

@Serializable
data class RuleEvaluation(
    val id: String,
    val name: String,
    val category: SignalCategory,
    val isTriggered: Boolean,
    val scoreContribution: Int,
    val primaryReason: String? = null,
    val weakeningReason: String? = null
) {
    val ruleId: String get() = id
    val ruleName: String get() = name
}
