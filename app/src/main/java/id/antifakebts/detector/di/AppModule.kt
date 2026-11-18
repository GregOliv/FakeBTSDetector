package id.antifakebts.detector.di

import android.telephony.TelephonyManager
import androidx.room.Room
import id.antifakebts.detector.data.export.DataExporter
import id.antifakebts.detector.data.local.AppDatabase
import id.antifakebts.detector.data.opencellid.OpenCellIdClient
import id.antifakebts.detector.data.repository.AnomalyRepository
import id.antifakebts.detector.data.repository.BaselineRepository
import id.antifakebts.detector.data.repository.CellRepository
import id.antifakebts.detector.data.repository.SettingsRepository
import id.antifakebts.detector.device.DevicePolicyManagerAdapter
import id.antifakebts.detector.domain.engine.HysteresisTracker
import id.antifakebts.detector.domain.engine.MovementContextEvaluator
import id.antifakebts.detector.domain.engine.PersistenceFilter
import id.antifakebts.detector.domain.engine.RuleEngine
import id.antifakebts.detector.domain.engine.RuleEngineConfig
import id.antifakebts.detector.domain.rules.CellularRule
import id.antifakebts.detector.domain.rules.DowngradeRule
import id.antifakebts.detector.domain.rules.NeighborListRule
import id.antifakebts.detector.domain.rules.PlmnMismatchRule
import id.antifakebts.detector.domain.rules.SuddenSignalRule
import id.antifakebts.detector.domain.rules.TacMismatchRule
import id.antifakebts.detector.domain.rules.UnknownCellRule
import id.antifakebts.detector.service.CellInfoParser
import id.antifakebts.detector.service.LocationTracker
import id.antifakebts.detector.ui.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    single { get<AppDatabase>().cellObservationDao() }
    single { get<AppDatabase>().cellBaselineDao() }
    single { get<AppDatabase>().anomalyLogDao() }

    single { SettingsRepository(androidContext()) }
    single { CellRepository(get()) }
    single { BaselineRepository(get()) }
    single { AnomalyRepository(get()) }

    single { CellInfoParser() }
    single { LocationTracker(androidContext()) }
    single { RuleEngineConfig() }
    single { MovementContextEvaluator() }
    single { PersistenceFilter(get<RuleEngineConfig>().persistenceConsecutiveReadings, get<RuleEngineConfig>().persistenceDurationSeconds) }
    single { HysteresisTracker(get()) }

    single { DevicePolicyManagerAdapter(androidContext(), get(), get()) }
    single { DataExporter(androidContext(), get()) }
    single { OpenCellIdClient(androidContext()) }

    single<List<CellularRule>> {
        val tm = androidContext().getSystemService(android.content.Context.TELEPHONY_SERVICE) as? TelephonyManager
        listOf(
            DowngradeRule(),
            UnknownCellRule(get()),
            SuddenSignalRule(),
            TacMismatchRule(get()),
            NeighborListRule(),
            PlmnMismatchRule(simPlmnProvider = { tm?.simOperator?.takeIf { it.isNotBlank() } })
        )
    }

    single {
        RuleEngine(
            rules = get(),
            movementEvaluator = get(),
            persistenceFilter = get(),
            hysteresisTracker = get(),
            config = get(),
            appInstallTimestampProvider = { get<SettingsRepository>().getAppInstallTime() }
        )
    }

    viewModel {
        MainViewModel(
            cellRepository = get(),
            baselineRepository = get(),
            anomalyRepository = get(),
            settingsRepository = get(),
            devicePolicyAdapter = get(),
            dataExporter = get()
        )
    }
}
