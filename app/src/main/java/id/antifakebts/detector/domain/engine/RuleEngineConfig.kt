package id.antifakebts.detector.domain.engine

data class RuleEngineConfig(
    val persistenceConsecutiveReadings: Int = 3,
    val persistenceDurationSeconds: Long = 10L,
    val highRiskThresholdScore: Int = 70,
    val mediumRiskThresholdScore: Int = 40,
    val lowRiskThresholdScore: Int = 15,
    val hysteresisReduction: Int = 15,
    val alertCooldownSeconds: Long = 300L,
    val learningModeDays: Int = 7,
    val maxMitigationsPerHour: Int = 2,
    val autoRevertDurationMinutes: Int = 10,
    val absoluteMitigationTimeoutMinutes: Int = 15
)
