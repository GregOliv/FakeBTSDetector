package id.antifakebts.detector.domain.engine

class PersistenceFilter(
    private val requiredConsecutiveReadings: Int = 3,
    private val requiredDurationSeconds: Long = 10L
) {
    private var consecutiveAnomalousCount = 0
    private var firstAnomalyTimestamp: Long = 0L

    fun recordEvaluation(isSuspicious: Boolean, timestamp: Long): Boolean {
        if (!isSuspicious) {
            consecutiveAnomalousCount = 0
            firstAnomalyTimestamp = 0L
            return false
        }

        if (consecutiveAnomalousCount == 0) {
            firstAnomalyTimestamp = timestamp
        }
        consecutiveAnomalousCount++

        val durationElapsed = (timestamp - firstAnomalyTimestamp) >= (requiredDurationSeconds * 1000)
        val countSatisfied = consecutiveAnomalousCount >= requiredConsecutiveReadings

        return countSatisfied || durationElapsed
    }

    fun reset() {
        consecutiveAnomalousCount = 0
        firstAnomalyTimestamp = 0L
    }
}
