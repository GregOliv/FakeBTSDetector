package id.antifakebts.detector.domain

import id.antifakebts.detector.data.local.dao.CellBaselineDao
import id.antifakebts.detector.data.local.entity.CellBaselineEntity
import id.antifakebts.detector.data.model.AnomalyLevel
import id.antifakebts.detector.data.model.CellObservation
import id.antifakebts.detector.data.model.ConfidenceLevel
import id.antifakebts.detector.data.model.NetworkType
import id.antifakebts.detector.data.repository.BaselineRepository
import id.antifakebts.detector.domain.engine.HysteresisTracker
import id.antifakebts.detector.domain.engine.MovementContextEvaluator
import id.antifakebts.detector.domain.engine.PersistenceFilter
import id.antifakebts.detector.domain.engine.RuleEngine
import id.antifakebts.detector.domain.engine.RuleEngineConfig
import id.antifakebts.detector.domain.rules.DowngradeRule
import id.antifakebts.detector.domain.rules.NeighborListRule
import id.antifakebts.detector.domain.rules.PlmnMismatchRule
import id.antifakebts.detector.domain.rules.SuddenSignalRule
import id.antifakebts.detector.domain.rules.TacMismatchRule
import id.antifakebts.detector.domain.rules.UnknownCellRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleEngineTest {

    private val baselineDao: CellBaselineDao = mockk(relaxed = true)
    private lateinit var baselineRepository: BaselineRepository
    private lateinit var ruleEngine: RuleEngine

    @Before
    fun setup() {
        baselineRepository = BaselineRepository(baselineDao)
        val rules = listOf(
            DowngradeRule(defaultWeight = 40),
            UnknownCellRule(baselineRepository, defaultWeight = 30),
            SuddenSignalRule(defaultWeight = 30),
            TacMismatchRule(baselineRepository, defaultWeight = 35),
            NeighborListRule(defaultWeight = 25),
            PlmnMismatchRule(simPlmnProvider = { "51010" }, defaultWeight = 35)
        )

        // Install date 30 days ago to test full non-learning mode behavior
        val pastInstallTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        ruleEngine = RuleEngine(
            rules = rules,
            movementEvaluator = MovementContextEvaluator(),
            persistenceFilter = PersistenceFilter(requiredConsecutiveReadings = 1, requiredDurationSeconds = 1),
            hysteresisTracker = HysteresisTracker(),
            config = RuleEngineConfig(
                highRiskThresholdScore = 65,
                mediumRiskThresholdScore = 35,
                lowRiskThresholdScore = 15
            ),
            appInstallTimestampProvider = { pastInstallTime }
        )
    }

    @Test
    fun singleRuleTriggered_shouldNeverYieldHighRisk() = runBlocking {
        // Only PLMN mismatch triggered (category: CELL_IDENTITY). Score = 35.
        // Rule: High risk strictly requires >= 2 distinct categories.
        val current = CellObservation(
            timestamp = 100000L,
            networkType = NetworkType.LTE,
            cellId = 1024L,
            lacTac = 4100,
            plmn = "51099", // mismatch
            signalDbm = -85,
            neighborCount = 3,
            isRegistered = true
        )
        coEvery { baselineDao.getByCellId(1024L) } returns listOf(
            CellBaselineEntity(
                gridKey = "grid", cellId = 1024L, lacTac = 4100, plmn = "51010",
                networkType = NetworkType.LTE, firstSeenTimestamp = 0, lastSeenTimestamp = 0,
                observationCount = 10, avgSignalDbm = -85.0, minSignalDbm = -90, maxSignalDbm = -80
            )
        )

        val result = ruleEngine.evaluate(current, emptyList())
        assertEquals(AnomalyLevel.MEDIUM, result.riskLevel)
        assertTrue(result.riskLevel != AnomalyLevel.HIGH)
    }

    @Test
    fun multiCategoryRulesTriggered_yieldsHighRiskAndHighConfidence() = runBlocking {
        // Trigger 2 rules from 2 different categories:
        // 1. DowngradeRule (NETWORK_BEHAVIOR): Forced 2G downgrade from strong 4G (score 40)
        // 2. UnknownCellRule (CELL_IDENTITY): Unknown 2G cell with 0 neighbors (score 30)
        // Total score = 70 (>= 65 threshold), 2 distinct categories.
        coEvery { baselineDao.getByCellId(9999L) } returns emptyList()

        val history = listOf(
            CellObservation(
                timestamp = 90000L,
                networkType = NetworkType.LTE,
                cellId = 1024L,
                lacTac = 4100,
                plmn = "51010",
                signalDbm = -75,
                signalRsrp = -78,
                neighborCount = 4,
                isRegistered = true
            )
        )

        val current = CellObservation(
            timestamp = 100000L,
            networkType = NetworkType.GSM,
            cellId = 9999L,
            lacTac = 999,
            plmn = "51010",
            signalDbm = -65,
            neighborCount = 0,
            isRegistered = true
        )

        val result = ruleEngine.evaluate(current, history)
        assertEquals(AnomalyLevel.HIGH, result.riskLevel)
        assertEquals(ConfidenceLevel.HIGH, result.confidence)
        assertTrue(result.primaryReasons.isNotEmpty())
    }

    @Test
    fun handlingUnavailableAndExtremeValues_doesNotCrashOrFalseAlert() = runBlocking {
        // Android telephony returning UNAVAILABLE values (2147483647 / Long.MAX_VALUE)
        val current = CellObservation(
            timestamp = 100000L,
            networkType = NetworkType.LTE,
            cellId = null,
            lacTac = null,
            plmn = null,
            signalDbm = null,
            signalRsrp = null,
            neighborCount = 0,
            isRegistered = true
        )

        val result = ruleEngine.evaluate(current, emptyList())
        assertEquals(AnomalyLevel.SAFE, result.riskLevel)
        assertFalse(result.shouldNotify)
    }
}
