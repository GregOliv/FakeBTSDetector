package id.antifakebts.detector.domain

import id.antifakebts.detector.data.local.dao.CellBaselineDao
import id.antifakebts.detector.data.model.AnomalyLevel
import id.antifakebts.detector.data.model.CellObservation
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FalsePositiveScenariosTest {

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
            persistenceFilter = PersistenceFilter(requiredConsecutiveReadings = 1, requiredDurationSeconds = 1),
            hysteresisTracker = HysteresisTracker(),
            config = RuleEngineConfig(
                highRiskThresholdScore = 65,
                mediumRiskThresholdScore = 35,
                lowRiskThresholdScore = 15
            ),
            appInstallTimestampProvider = { 0L }
        )
    }

    @Test
    fun scenario1_coverageEdgeFallback_producesSafeOrLowRisk() = runBlocking {
        // User moves to edge of coverage. 4G signal was deteriorating (-114 dBm), then phone falls back to 2G.
        val history = listOf(
            CellObservation(
                timestamp = 90000L,
                networkType = NetworkType.LTE,
                cellId = 1001L,
                lacTac = 4100,
                plmn = "51010",
                signalDbm = -114,
                signalRsrp = -116,
                neighborCount = 1,
                isRegistered = true
            )
        )

        val current2G = CellObservation(
            timestamp = 100000L,
            networkType = NetworkType.GSM,
            cellId = 2002L,
            lacTac = 4100,
            plmn = "51010",
            signalDbm = -95,
            neighborCount = 2,
            isRegistered = true
        )
        coEvery { baselineDao.getByCellId(2002L) } returns emptyList()

        val result = ruleEngine.evaluate(current2G, history)
        // Must NOT be HIGH. Weakening factor explains natural coverage edge fallback.
        assertTrue(result.riskLevel == AnomalyLevel.SAFE || result.riskLevel == AnomalyLevel.LOW)
        assertTrue(result.weakeningReasons.any { it.contains("Transisi 2G wajar") })
    }

    @Test
    fun scenario2_elevatorBasementSignalDrop_producesSafeOrLowRisk() = runBlocking {
        // Entering elevator: 4G signal attenuates to -118 dBm with 0 neighbors due to concrete shaft
        val history = listOf(
            CellObservation(
                timestamp = 90000L,
                networkType = NetworkType.LTE,
                cellId = 1001L,
                lacTac = 4100,
                plmn = "51010",
                signalDbm = -105,
                neighborCount = 2,
                isRegistered = true
            )
        )

        val currentDeepIndoor = CellObservation(
            timestamp = 100000L,
            networkType = NetworkType.LTE,
            cellId = 1001L,
            lacTac = 4100,
            plmn = "51010",
            signalDbm = -118,
            neighborCount = 0, // 0 neighbors is expected in deep indoor/elevator
            isRegistered = true
        )
        coEvery { baselineDao.getByCellId(1001L) } returns mockk(relaxed = true)

        val result = ruleEngine.evaluate(currentDeepIndoor, history)
        assertEquals(AnomalyLevel.SAFE, result.riskLevel)
    }

    @Test
    fun scenario3_vehicleCommuteMovement_ignoresCellChangesAsNormalHandover() = runBlocking {
        // User driving in vehicle. Location changes from (lat1, lon1) to (lat2, lon2) distance > 1.5 km
        val prevLocation = CellObservation(
            timestamp = 60000L,
            networkType = NetworkType.LTE,
            cellId = 1001L,
            lacTac = 4100,
            plmn = "51010",
            signalDbm = -85,
            latitude = -6.2000,
            longitude = 106.8166,
            isRegistered = true
        )

        val currentLocationMoved = CellObservation(
            timestamp = 100000L,
            networkType = NetworkType.LTE,
            cellId = 3003L, // New cell
            lacTac = 4200, // Different TAC
            plmn = "51010",
            signalDbm = -75,
            latitude = -6.2150, // Moved ~1.7 km in 40s
            longitude = 106.8280,
            neighborCount = 3,
            isRegistered = true
        )
        coEvery { baselineDao.getByCellId(3003L) } returns emptyList()

        val result = ruleEngine.evaluate(currentLocationMoved, listOf(prevLocation))
        // Movement context suppresses TAC mismatch and sudden signal rules
        assertTrue(result.riskLevel != AnomalyLevel.HIGH)
    }

    @Test
    fun scenario4_legitimateNewCellWithNeighbors_producesLowOrSafeRisk() = runBlocking {
        // A new tower deployed by operator. Not in baseline yet, but has 4 normal neighbors and valid PLMN
        val current = CellObservation(
            timestamp = 100000L,
            networkType = NetworkType.LTE,
            cellId = 88888L,
            lacTac = 4100,
            plmn = "51010",
            signalDbm = -80,
            neighborCount = 4,
            isRegistered = true
        )
        coEvery { baselineDao.getByCellId(88888L) } returns emptyList()

        val result = ruleEngine.evaluate(current, emptyList())
        assertTrue(result.riskLevel == AnomalyLevel.SAFE || result.riskLevel == AnomalyLevel.LOW)
        assertTrue(result.weakeningReasons.any { it.contains("sel tetangga wajar") })
    }

    @Test
    fun scenario5_internationalRoaming_producesSafeOrLowRisk() = runBlocking {
        // SIM is Telkomsel (51010), but user is in Singapore connected to Singtel (52501) with roaming flag active
        val current = CellObservation(
            timestamp = 100000L,
            networkType = NetworkType.LTE,
            cellId = 4004L,
            lacTac = 200,
            plmn = "52501", // Mismatch with SIM
            signalDbm = -78,
            neighborCount = 3,
            isRegistered = true
        )
        coEvery { baselineDao.getByCellId(4004L) } returns emptyList()

        val result = ruleEngine.evaluate(current, emptyList(), isRoaming = true)
        assertEquals(AnomalyLevel.SAFE, result.riskLevel)
        assertTrue(result.weakeningReasons.any { it.contains("status roaming") })
    }
}
