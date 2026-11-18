package id.antifakebts.detector.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.antifakebts.detector.data.local.entity.AnomalyLogEntity
import id.antifakebts.detector.data.model.AnomalyLevel
import id.antifakebts.detector.data.model.CellObservation
import id.antifakebts.detector.data.model.ConfidenceLevel
import id.antifakebts.detector.device.MitigationStatus
import id.antifakebts.detector.domain.engine.EvaluationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

enum class NavigationTab(val label: String) {
    STATUS("Status"),
    TIMELINE("Riwayat"),
    INSPECTOR("Sinyal"),
    SETTINGS("Setelan")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF81C784),
                    secondary = Color(0xFFFFB74D),
                    surface = Color(0xFF1E1E1E),
                    background = Color(0xFF121212)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF1B5E20),
                    secondary = Color(0xFFE65100),
                    surface = Color(0xFFFFFFFF),
                    background = Color(0xFFF8F9FA)
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRootScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRootScreen(viewModel: MainViewModel = koinViewModel()) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(NavigationTab.STATUS) }

    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val activeRisk by viewModel.activeRiskLevel.collectAsStateWithLifecycle()
    val latestEval by viewModel.latestEvaluation.collectAsStateWithLifecycle()
    val servingCell by viewModel.currentServingCell.collectAsStateWithLifecycle()
    val neighbors by viewModel.neighboringCells.collectAsStateWithLifecycle()
    val anomalyLogs by viewModel.anomalyLogs.collectAsStateWithLifecycle()
    val mitigationStatus by viewModel.mitigationStatus.collectAsStateWithLifecycle()
    val isAutoMitigationEnabled by viewModel.isAutoMitigationEnabled.collectAsStateWithLifecycle()
    val isSmsScanEnabled by viewModel.isSmsScanEnabled.collectAsStateWithLifecycle()
    val totalObservations by viewModel.totalObservations.collectAsStateWithLifecycle()
    val totalLearned by viewModel.totalLearnedCells.collectAsStateWithLifecycle()
    val totalAlerts by viewModel.totalAlerts.collectAsStateWithLifecycle()

    var hasPermissions by remember { mutableStateOf(checkAllPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        hasPermissions = map.values.all { it }
        if (hasPermissions && !isServiceRunning) {
            viewModel.startMonitoring(context)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(getRequiredPermissions())
        } else if (!isServiceRunning) {
            viewModel.startMonitoring(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FakeBTS Detector",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            when (tab) {
                                NavigationTab.STATUS -> Icon(Icons.Default.Security, contentDescription = null)
                                NavigationTab.TIMELINE -> Icon(Icons.Default.History, contentDescription = null)
                                NavigationTab.INSPECTOR -> Icon(Icons.Default.CellTower, contentDescription = null)
                                NavigationTab.SETTINGS -> Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        },
                        label = { Text(tab.label, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                NavigationTab.STATUS -> StatusDashboardScreen(
                    hasPermissions = hasPermissions,
                    isServiceRunning = isServiceRunning,
                    activeRisk = activeRisk,
                    latestEval = latestEval,
                    servingCell = servingCell,
                    mitigationStatus = mitigationStatus,
                    onStart = {
                        if (hasPermissions) viewModel.startMonitoring(context)
                        else permissionLauncher.launch(getRequiredPermissions())
                    },
                    onStop = { viewModel.stopMonitoring(context) },
                    onManualOverride = { viewModel.manualOverride() },
                    onRequestPermissions = { permissionLauncher.launch(getRequiredPermissions()) }
                )

                NavigationTab.TIMELINE -> TimelineScreen(
                    logs = anomalyLogs,
                    onMarkSafe = { viewModel.markLogSafe(it) }
                )

                NavigationTab.INSPECTOR -> CellInspectorScreen(
                    servingCell = servingCell,
                    neighbors = neighbors,
                    isServiceRunning = isServiceRunning
                )

                NavigationTab.SETTINGS -> SettingsScreen(
                    mitigationStatus = mitigationStatus,
                    isAutoMitigationEnabled = isAutoMitigationEnabled,
                    isSmsScanEnabled = isSmsScanEnabled,
                    totalObservations = totalObservations,
                    totalLearned = totalLearned,
                    totalAlerts = totalAlerts,
                    onToggleAutoMitigation = { viewModel.setAutoMitigationEnabled(it) },
                    onToggleSmsScan = { viewModel.setSmsScanEnabled(it) },
                    onExportCsv = { viewModel.exportAndShareCsv() },
                    onExportJson = { viewModel.exportAndShareJson() }
                )
            }
        }
    }
}

@Composable
fun StatusDashboardScreen(
    hasPermissions: Boolean,
    isServiceRunning: Boolean,
    activeRisk: AnomalyLevel,
    latestEval: EvaluationResult?,
    servingCell: CellObservation?,
    mitigationStatus: MitigationStatus,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onManualOverride: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!hasPermissions) {
            item {
                PermissionNoticeCard(onRequestPermissions = onRequestPermissions)
            }
        }

        // Active mitigation safeguard banner
        if (mitigationStatus.isAutoMitigationActive) {
            item {
                ActiveMitigationBanner(
                    status = mitigationStatus,
                    onManualOverride = onManualOverride
                )
            }
        }

        // Primary status card
        item {
            PrimarySecurityCard(
                isServiceRunning = isServiceRunning,
                risk = activeRisk,
                latestEval = latestEval,
                servingCell = servingCell
            )
        }

        // Non-Device Owner guidance card on HIGH risk
        if (activeRisk == AnomalyLevel.HIGH && !mitigationStatus.isDeviceOwner) {
            item {
                NonDeviceOwnerGuidanceCard(
                    onOpenNetworkSettings = {
                        val intent = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }
        }

        // Service toggle controls
        item {
            ServiceControlCard(
                isServiceRunning = isServiceRunning,
                onStart = onStart,
                onStop = onStop
            )
        }
    }
}

@Composable
fun PrimarySecurityCard(
    isServiceRunning: Boolean,
    risk: AnomalyLevel,
    latestEval: EvaluationResult?,
    servingCell: CellObservation?
) {
    val isDark = isSystemInDarkTheme()
    val (bgColor, accentColor, titleText, subtitleText) = when {
        !isServiceRunning -> Quadruple(
            if (isDark) Color(0xFF262626) else Color(0xFFF1F5F9),
            Color.Gray,
            "Pemantauan Berhenti",
            "Aktifkan layanan pemantauan untuk mulai memeriksa radio seluler."
        )
        risk == AnomalyLevel.HIGH -> Quadruple(
            if (isDark) Color(0xFF3B1A1A) else Color(0xFFFFEBEE),
            Color(0xFFC62828),
            "Kemungkinan Anomali Berisiko",
            latestEval?.primaryReasons?.firstOrNull() ?: "Terdeteksi pola perilaku seluler yang tidak lazim."
        )
        risk == AnomalyLevel.MEDIUM -> Quadruple(
            if (isDark) Color(0xFF3B2A18) else Color(0xFFFFF3E0),
            Color(0xFFE65100),
            "Ada Tanda yang Tidak Biasa",
            latestEval?.primaryReasons?.firstOrNull() ?: "Terdeteksi inkonsistensi ringan pada konfigurasi sel."
        )
        risk == AnomalyLevel.LOW -> Quadruple(
            if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
            Color(0xFF0284C7),
            "Fluktuasi Ringan Tercatat",
            "Koneksi berpindah sel atau sinyal berubah dalam batas toleransi normal."
        )
        else -> Quadruple(
            if (isDark) Color(0xFF1B3B22) else Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            "Koneksi Terpantau Normal",
            "Sel terdaftar di baseline dan parameter radio sesuai spesifikasi operator."
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(accentColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = titleText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitleText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            // Weakening reasons (transparency on counter-evidence)
            if (latestEval?.weakeningReasons?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Faktor yang melemahkan dugaan:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                latestEval.weakeningReasons.take(2).forEach { reason ->
                    Text(
                        text = "• $reason",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }

            if (servingCell != null && isServiceRunning) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${servingCell.operatorAlpha ?: "Operator"} (${servingCell.networkType.displayName})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "CID: ${servingCell.getDisplayCellId()}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveMitigationBanner(
    status: MitigationStatus,
    onManualOverride: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFC62828))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mitigasi Otomatis Sedang Aktif",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pembatasan radio 2G dan SMS diberlakukan untuk melindungi perangkat dari IMSI catcher.",
                fontSize = 12.sp,
                color = Color.Black
            )
            val minutes = status.autoRevertRemainingSeconds / 60
            val seconds = status.autoRevertRemainingSeconds % 60
            Text(
                text = "Auto-revert otomatis dalam: %02d:%02d".format(minutes, seconds),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC62828)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onManualOverride,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Text("Cabut Pembatasan Sekarang (Manual Override)")
            }
        }
    }
}

@Composable
fun NonDeviceOwnerGuidanceCard(onOpenNetworkSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Panduan Tindakan Cepat (Non-Root)",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "1. Buka pengaturan jaringan dan nonaktifkan opsi 'Izinkan 2G' jika tersedia.\n2. Hindari memasukkan kode OTP atau menekan tautan SMS saat status ini aktif.",
                fontSize = 12.sp,
                color = Color.Black,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onOpenNetworkSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Text("Buka Pengaturan Jaringan")
            }
        }
    }
}

@Composable
fun TimelineScreen(
    logs: List<AnomalyLogEntity>,
    onMarkSafe: (AnomalyLogEntity) -> Unit
) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Belum Ada Catatan Anomali",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Aplikasi terus memantau sinyal seluler di latar belakang.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(logs) { log ->
                TimelineLogCard(log = log, onMarkSafe = { onMarkSafe(log) })
            }
        }
    }
}

@Composable
fun TimelineLogCard(
    log: AnomalyLogEntity,
    onMarkSafe: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(log.timestamp))

    val badgeColor = when (log.riskLevel) {
        AnomalyLevel.HIGH -> Color(0xFFC62828)
        AnomalyLevel.MEDIUM -> Color(0xFFE65100)
        AnomalyLevel.LOW -> Color(0xFF0284C7)
        AnomalyLevel.SAFE -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(badgeColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Risiko ${log.riskLevel.labelId} (Keyakinan: ${log.confidence.labelId})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = badgeColor
                    )
                }
                Text(text = formattedDate, fontSize = 11.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = log.primaryReason, fontSize = 13.sp, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${log.networkType.name} | CID: ${log.cellId ?: "-"} | TAC: ${log.lacTac ?: "-"}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.DarkGray
                )
                Text(
                    text = log.signalDbm?.let { "$it dBm" } ?: "",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!log.userMarkedSafe) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onMarkSafe,
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Tandai Sel Ini Aman (Whitelist)", fontSize = 11.sp)
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ Telah ditandai aman oleh pengguna",
                    fontSize = 11.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CellInspectorScreen(
    servingCell: CellObservation?,
    neighbors: List<CellObservation>,
    isServiceRunning: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ServingCellCard(cell = servingCell, isServiceRunning = isServiceRunning)
        }

        item {
            Text(
                text = "Sel Tetangga Terdeteksi (${neighbors.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        if (neighbors.isEmpty()) {
            item {
                Text(
                    text = "Tidak ada sel tetangga yang disiarkan atau terdeteksi oleh modem.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        } else {
            items(neighbors) { neighbor ->
                NeighborCellRow(cell = neighbor)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    mitigationStatus: MitigationStatus,
    isAutoMitigationEnabled: Boolean,
    isSmsScanEnabled: Boolean,
    totalObservations: Int,
    totalLearned: Int,
    totalAlerts: Int,
    onToggleAutoMitigation: (Boolean) -> Unit,
    onToggleSmsScan: (Boolean) -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Status Perangkat & Device Policy", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (mitigationStatus.isDeviceOwner) {
                            "Status: Terdaftar sebagai Device Owner (Akses kebijakan mitigasi 2G & SMS aktif)"
                        } else {
                            "Status: Aplikasi Biasa (Mode Device Owner tidak aktif; mitigasi otomatis dibatasi oleh Android OS)"
                        },
                        fontSize = 12.sp,
                        color = if (mitigationStatus.isDeviceOwner) Color(0xFF2E7D32) else Color.DarkGray
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Opsi Respons Keamanan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Mitigasi Otomatis (Device Owner)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Batasi 2G dan SMS sementara saat anomali Tinggi. Peringatan: SMS OTP juga akan tertahan.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isAutoMitigationEnabled,
                            onCheckedChange = onToggleAutoMitigation,
                            enabled = mitigationStatus.isDeviceOwner
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Pindai Tautan SMS Mencurigakan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Mendeteksi tautan URL pada SMS masuk saat jaringan berada dalam status anomali.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isSmsScanEnabled,
                            onCheckedChange = onToggleSmsScan
                        )
                    }
                }
            }
        }

        item {
            SystemMetricsCard(
                totalObservations = totalObservations,
                totalLearned = totalLearned,
                totalAlerts = totalAlerts
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Ekspor Log Keamanan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportCsv,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ekspor CSV", fontSize = 12.sp)
                        }
                        Button(
                            onClick = onExportJson,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ekspor JSON", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ServiceControlCard(
    isServiceRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isServiceRunning) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (isServiceRunning) Color(0xFF2E7D32) else Color(0xFFC62828),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isServiceRunning) "Status: Memantau" else "Status: Berhenti",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    Text(
                        text = if (isServiceRunning) "Foreground Service Aktif" else "Tekan mulai untuk inspeksi",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }

            if (isServiceRunning) {
                OutlinedButton(
                    onClick = onStop,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hentikan")
                }
            } else {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mulai")
                }
            }
        }
    }
}

@Composable
fun ServingCellCard(cell: CellObservation?, isServiceRunning: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CellTower,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sel Seluler Aktif (Serving Cell)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isServiceRunning) {
                Text(
                    text = "Layanan pemantauan sedang tidak aktif. Aktifkan layanan di atas untuk melihat data seluler.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (cell == null) {
                Text(
                    text = "Menunggu sinyal radio seluler dari modem HP...",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                InfoRow("Operator / PLMN", "${cell.operatorAlpha ?: "-"} (${cell.plmn ?: "-"})")
                InfoRow("Teknologi Jaringan", cell.networkType.displayName)
                InfoRow("Cell ID (CID / NCI)", cell.getDisplayCellId())
                InfoRow("Area Code (TAC / LAC)", cell.getDisplayTac())
                InfoRow("Kekuatan Sinyal", cell.signalDbm?.let { "$it dBm" } ?: "Tidak Terbaca")
                cell.signalRsrp?.let {
                    InfoRow("RSRP (LTE/5G)", "$it dBm")
                }
                cell.channelNumber?.let {
                    InfoRow("Saluran Frekuensi", it.toString())
                }
                cell.physicalCellId?.let {
                    InfoRow("Physical Cell ID (PCI)", it.toString())
                }
                InfoRow("Sel Tetangga Terdeteksi", "${cell.neighborCount} sel")
                cell.latitude?.let { lat ->
                    cell.longitude?.let { lon ->
                        InfoRow("Koordinat Kasar", "%.4f, %.4f".format(lat, lon))
                    }
                }
            }
        }
    }
}

@Composable
fun SystemMetricsCard(
    totalObservations: Int,
    totalLearned: Int,
    totalAlerts: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Statistik Penyimpanan Lokal (Room)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn("Total Observasi", totalObservations.toString())
                MetricColumn("Sel Normal Terdata", totalLearned.toString())
                MetricColumn("Peringatan Anomali", totalAlerts.toString())
            }
        }
    }
}

@Composable
fun MetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun NeighborCellRow(cell: CellObservation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${cell.networkType.name} | CID: ${cell.getDisplayCellId()}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Text(
                    text = "TAC/LAC: ${cell.getDisplayTac()} | PCI: ${cell.physicalCellId ?: "-"}",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }
            Text(
                text = cell.signalDbm?.let { "$it dBm" } ?: "-",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PermissionNoticeCard(onRequestPermissions: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Izin Diperlukan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFFE65100)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Android mewajibkan izin Lokasi Presisi dan Status Telepon untuk membaca identitas Cell ID dan sinyal radio seluler.",
                fontSize = 12.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Text("Berikan Izin")
            }
        }
    }
}

fun checkAllPermissions(context: Context): Boolean {
    val fineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val phoneState = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED

    val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    return fineLocation && phoneState && notifications
}

fun getRequiredPermissions(): Array<String> {
    val list = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return list.toTypedArray()
}

