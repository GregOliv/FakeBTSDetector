package id.antifakebts.detector.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import id.antifakebts.detector.data.local.entity.AnomalyLogEntity
import id.antifakebts.detector.data.repository.AnomalyRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataExporter(
    private val context: Context,
    private val anomalyRepository: AnomalyRepository
) {

    suspend fun exportToCsv(): File {
        val logs = anomalyRepository.getAllLogsForExport()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        val fileName = "FakeBTS_Logs_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        file.bufferedWriter().use { writer ->
            writer.write("ID,Timestamp,Date,RiskLevel,Confidence,CellID,LAC_TAC,PLMN,NetworkType,SignalDbm,PrimaryReason,UserMarkedSafe\n")
            for (log in logs) {
                val isoDate = dateFormat.format(Date(log.timestamp))
                val sanitizedReason = "\"${log.primaryReason.replace("\"", "\"\"")}\""
                writer.write("${log.id},${log.timestamp},$isoDate,${log.riskLevel},${log.confidence},${log.cellId ?: ""},${log.lacTac ?: ""},${log.plmn ?: ""},${log.networkType},${log.signalDbm ?: ""},$sanitizedReason,${log.userMarkedSafe}\n")
            }
        }

        return file
    }

    suspend fun exportToJson(): File {
        val logs = anomalyRepository.getAllLogsForExport()
        val fileName = "FakeBTS_Logs_${System.currentTimeMillis()}.json"
        val file = File(context.cacheDir, fileName)

        val jsonString = Json { prettyPrint = true }.encodeToString(logs.map {
            mapOf(
                "id" to it.id.toString(),
                "timestamp" to it.timestamp.toString(),
                "riskLevel" to it.riskLevel.name,
                "confidence" to it.confidence.name,
                "cellId" to (it.cellId?.toString() ?: ""),
                "lacTac" to (it.lacTac?.toString() ?: ""),
                "plmn" to (it.plmn ?: ""),
                "networkType" to it.networkType.name,
                "signalDbm" to (it.signalDbm?.toString() ?: ""),
                "primaryReason" to it.primaryReason,
                "weakeningFactorsJson" to it.weakeningFactorsJson,
                "userMarkedSafe" to it.userMarkedSafe.toString()
            )
        })

        file.writeText(jsonString)
        return file
    }

    fun shareExportedFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.name.endsWith(".csv")) "text/csv" else "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Ekspor Log Keamanan FakeBTS").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
