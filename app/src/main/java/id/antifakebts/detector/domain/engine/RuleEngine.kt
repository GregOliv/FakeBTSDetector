package id.antifakebts.detector.domain.engine

import id.antifakebts.detector.data.model.AnomalyLevel
import id.antifakebts.detector.data.model.CellObservation
import id.antifakebts.detector.data.model.ConfidenceLevel
import id.antifakebts.detector.domain.rules.CellularRule
import id.antifakebts.detector.domain.rules.RuleEvaluation
import kotlinx.serialization.Serializable

@Serializable
data class EvaluationResult(
    val riskLevel: AnomalyLevel,
    val confidence: ConfidenceLevel,
    val rawScore: Int,
    val isPersistent: Boolean,
    val isLearningModeActive: Boolean,
    val primaryReasons: List<String>,
    val weakeningReasons: List<String>,
    val triggeredRules: List<RuleEvaluation>,
    val shouldNotify: Boolean
)

class RuleEngine(
    private val rules: List<CellularRule>,
    private val movementEvaluator: MovementContextEvaluator = MovementContextEvaluator(),
    private val persistenceFilter: PersistenceFilter = PersistenceFilter(),
    hysteresisTracker: HysteresisTracker? = null,
    private val config: RuleEngineConfig = RuleEngineConfig(),
    private val appInstallTimestampProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val hysteresisTracker: HysteresisTracker = hysteresisTracker ?: HysteresisTracker(config)

    suspend fun evaluate(
        current: CellObservation,
        history: List<CellObservation>,
        isRoaming: Boolean = false
    ): EvaluationResult {
        val now = current.timestamp

        // Determine movement context
        val prev = history.firstOrNull()
        val timeDelta = if (prev != null) (now - prev.timestamp) else 0L
        val isMoving = movementEvaluator.isMoving(
            currentLat = current.latitude,
            currentLon = current.longitude,
            prevLat = prev?.latitude,
            prevLon = prev?.longitude,
            timeDeltaMillis = timeDelta
        )

        // Evaluate all rules
        val evaluations = rules.map { rule ->
            rule.evaluate(current, history, isMoving, isRoaming)
        }

        val triggeredEvaluations = evaluations.filter { it.isTriggered }
        val rawScore = triggeredEvaluations.sumOf { it.scoreContribution }
        val distinctCategories = triggeredEvaluations.map { it.category }.distinct()

        val isSuspicious = rawScore >= config.lowRiskThresholdScore
        val isPersistent = persistenceFilter.recordEvaluation(isSuspicious, now)

        val computedLevel = this.hysteresisTracker.computeLevel(
            rawScore = rawScore,
            categoryCount = distinctCategories.size,
            isPersistent = isPersistent
        )

        // Check learning mode (shadow mode for first N days)
        val installTime = appInstallTimestampProvider()
        val elapsedMillis = now - installTime
        val isLearningModeActive = config.learningModeDays > 0 &&
            elapsedMillis >= 0L &&
            elapsedMillis < (config.learningModeDays * 24L * 60L * 60L * 1000L)

        // In learning mode, high alerts are suppressed to shadow log
        val effectiveLevel = if (isLearningModeActive && computedLevel == AnomalyLevel.HIGH) {
            AnomalyLevel.MEDIUM
        } else {
            computedLevel
        }

        val confidence = when {
            distinctCategories.size >= 2 && isPersistent && rawScore >= config.highRiskThresholdScore -> ConfidenceLevel.HIGH
            isPersistent || rawScore >= config.mediumRiskThresholdScore -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        val primaryReasons = triggeredEvaluations.mapNotNull { it.primaryReason }
        val weakeningReasons = evaluations.mapNotNull { it.weakeningReason }

        val canNotify = hysteresisTracker.canTriggerAlertNotification(effectiveLevel, now)
        val shouldNotify = canNotify && (!isLearningModeActive || effectiveLevel == AnomalyLevel.LOW)

        return EvaluationResult(
            riskLevel = effectiveLevel,
            confidence = confidence,
            rawScore = rawScore,
            isPersistent = isPersistent,
            isLearningModeActive = isLearningModeActive,
            primaryReasons = primaryReasons,
            weakeningReasons = weakeningReasons,
            triggeredRules = triggeredEvaluations,
            shouldNotify = shouldNotify
        )
    }

    fun reset() {
        persistenceFilter.reset()
        hysteresisTracker.reset()
    }
}
