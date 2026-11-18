package id.antifakebts.detector.domain

import id.antifakebts.detector.data.local.dao.CellBaselineDao
import id.antifakebts.detector.data.local.entity.CellBaselineEntity
import id.antifakebts.detector.data.model.AnomalyLevel
import id.antifakebts.detector.data.model.CellObservation
import id.antifakebts.detector.data.model.NetworkType
import id.antifakebts.detector.data.repository.BaselineRepository
import id.antifakebts.detector.domain.engine.EvaluationResult
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReplayEngineTest {

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

        ruleEngine = RuleEngine(
            rules = rules,
            movementEvaluator = MovementContextEvaluator(),
            persistenceFilter = PersistenceFilter(requiredConsecutiveReadings = 3, requiredDurationSeconds = 10),
            hysteresisTracker = HysteresisTracker(),
            config = RuleEngineConfig(
                highRiskThresholdScore = 65,
                mediumRiskThresholdScore = 35,
                lowRiskThresholdScore = 15,
                alertCooldownSeconds = 300
            ),
            appInstallTimestampProvider = { 0L }
        )
    }

    @Test
    fun replaySequence_simulatingNormalTrafficThenAttackThenRecovery() = runBlocking {
        // Setup baseline for legitimate cell 1024
        coEvery { baselineDao.getByCellId(1024L) } returns listOf(
            CellBaselineEntity(
                gridKey = "-6.20_106.81_51010_1024",
                cellId = 1024L,
                lacTac = 4100,
                plmn = "51010",
                networkType = NetworkType.LTE,
                firstSeenTimestamp = 0L,
                lastSeenTimestamp = 0L,
                observationCount = 50,
                avgSignalDbm = -82.0,
                minSignalDbm = -90,
                maxSignalDbm = -75
            )
        )
        // Unknown rogue cell 99999
        coEvery { baselineDao.getByCellId(99999L) } returns emptyList()

        val results = mutableListOf<EvaluationResult>()
        val history = mutableListOf<CellObservation>()

        // 1. Replay 5 normal 4G samples
        for (i in 1..5) {
            val normalSample = CellObservation(
                timestamp = i * 2000L,
                networkType = NetworkType.LTE,
                cellId = 1024L,
                lacTac = 4100,
                plmn = "51010",
                signalDbm = -82,
                signalRsrp = -85,
                neighborCount = 3,
                latitude = -6.2000,
                longitude = 106.8100,
                isRegistered = true
            )
            val result = ruleEngine.evaluate(normalSample, history)
            results.add(result)
            history.add(0, normalSample)
        }

        // All normal samples should be SAFE
        assertTrue(results.all { it.riskLevel == AnomalyLevel.SAFE })

        // 2. Replay Rogue BTS attack: sudden surge to -55 dBm, 2G GSM, 0 neighbors, unknown cell ID
        val attackResults = mutableListOf<EvaluationResult>()
        for (i in 6..9) {
            val rogueSample = CellObservation(
                timestamp = i * 2000L,
                networkType = NetworkType.GSM,
                cellId = 99999L,
                lacTac = 999,
                plmn = "51010",
                signalDbm = -55,
                neighborCount = 0,
                latitude = -6.2000,
                longitude = 106.8100,
                isRegistered = true
            )
            val result = ruleEngine.evaluate(rogueSample, history)
            attackResults.add(result)
            history.add(0, rogueSample)
        }

        // Reading 1 of attack (sample 6): persistence not yet satisfied (need 3 readings)
        assertEquals(AnomalyLevel.SAFE, attackResults[0].riskLevel)
        // Reading 3 of attack (sample 8): persistence satisfied, multi-category satisfied -> Escalates to HIGH
        assertEquals(AnomalyLevel.HIGH, attackResults[2].riskLevel)
        // Reading 4 of attack (sample 9): maintained HIGH
        assertEquals(AnomalyLevel.HIGH, attackResults[3].riskLevel)

        // 3. Replay Recovery: phone moves away or rogue powers down, reconnects to normal 4G cell 1024
        val recoveryResults = mutableListOf<EvaluationResult>()
        for (i in 10..15) {
            val recoverySample = CellObservation(
                timestamp = i * 2000L,
                networkType = NetworkType.LTE,
                cellId = 1024L,
                lacTac = 4100,
                plmn = "51010",
                signalDbm = -83,
                signalRsrp = -86,
                neighborCount = 3,
                latitude = -6.2000,
                longitude = 106.8100,
                isRegistered = true
            )
            val result = ruleEngine.evaluate(recoverySample, history)
            recoveryResults.add(result)
            history.add(0, recoverySample)
        }

        // Level de-escalates via hysteresis down to SAFE
        val finalResult = recoveryResults.last()
        assertEquals(AnomalyLevel.SAFE, finalResult.riskLevel)
    }
}
