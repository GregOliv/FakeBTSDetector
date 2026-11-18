package id.antifakebts.detector.domain.engine

import id.antifakebts.detector.data.model.AnomalyLevel

class HysteresisTracker(
    private val config: RuleEngineConfig = RuleEngineConfig()
) {
    private var currentLevel: AnomalyLevel = AnomalyLevel.SAFE
    private var lastAlertTimestamp: Long = 0L

    fun computeLevel(
        rawScore: Int,
        categoryCount: Int,
        isPersistent: Boolean
    ): AnomalyLevel {
        // Core anti false-positive rule: HIGH risk strictly requires >= 2 distinct categories
        val canBeHigh = categoryCount >= 2 && isPersistent

        val nextLevel = when (currentLevel) {
            AnomalyLevel.SAFE -> {
                when {
                    rawScore >= config.highRiskThresholdScore && canBeHigh -> AnomalyLevel.HIGH
                    rawScore >= config.mediumRiskThresholdScore && isPersistent -> AnomalyLevel.MEDIUM
                    rawScore >= config.lowRiskThresholdScore && isPersistent -> AnomalyLevel.LOW
                    else -> AnomalyLevel.SAFE
                }
            }

            AnomalyLevel.LOW -> {
                when {
                    rawScore >= config.highRiskThresholdScore && canBeHigh -> AnomalyLevel.HIGH
                    rawScore >= config.mediumRiskThresholdScore && isPersistent -> AnomalyLevel.MEDIUM
                    rawScore >= config.lowRiskThresholdScore -> AnomalyLevel.LOW
                    else -> AnomalyLevel.SAFE
                }
            }

            AnomalyLevel.MEDIUM -> {
                val fallThreshold = config.mediumRiskThresholdScore - config.hysteresisReduction
                when {
                    rawScore >= config.highRiskThresholdScore && canBeHigh -> AnomalyLevel.HIGH
                    rawScore < fallThreshold -> AnomalyLevel.LOW
                    else -> AnomalyLevel.MEDIUM
                }
            }

            AnomalyLevel.HIGH -> {
                val fallThreshold = config.highRiskThresholdScore - config.hysteresisReduction
                when {
                    rawScore < fallThreshold || !canBeHigh -> AnomalyLevel.MEDIUM
                    else -> AnomalyLevel.HIGH
                }
            }
        }

        currentLevel = nextLevel
        return currentLevel
    }

    fun canTriggerAlertNotification(newLevel: AnomalyLevel, timestamp: Long): Boolean {
        if (newLevel == AnomalyLevel.SAFE) return false
        val cooldownMs = config.alertCooldownSeconds * 1000
        val isCooldownPassed = (timestamp - lastAlertTimestamp) >= cooldownMs
        val isEscalation = newLevel.ordinal > currentLevel.ordinal

        if (isCooldownPassed || isEscalation) {
            lastAlertTimestamp = timestamp
            return true
        }
        return false
    }

    fun getCurrentLevel(): AnomalyLevel = currentLevel

    fun reset() {
        currentLevel = AnomalyLevel.SAFE
        lastAlertTimestamp = 0L
    }
}
