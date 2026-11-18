package id.antifakebts.detector.data.opencellid

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class OpenCellIdResult(
    val isValidTower: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rangeMeters: Int? = null,
    val errorMessage: String? = null
)

class OpenCellIdClient(
    private val context: Context,
    private val apiKeyProvider: () -> String? = { null }
) {
    private var lastRequestTimestamp: Long = 0L
    private val memoryCache = mutableMapOf<String, OpenCellIdResult>()

    suspend fun queryCell(
        mcc: Int,
        mnc: Int,
        lac: Int,
        cellId: Long
    ): OpenCellIdResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            return@withContext OpenCellIdResult(
                isValidTower = false,
                errorMessage = "API key OpenCellID belum dikonfigurasi di pengaturan"
            )
        }

        val cacheKey = "${mcc}_${mnc}_${lac}_${cellId}"
        memoryCache[cacheKey]?.let { return@withContext it }

        // Rate limit: OpenCellID free tier allows 1 request per second
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTimestamp
        if (elapsed < 1100L) {
            kotlinx.coroutines.delay(1100L - elapsed)
        }
        lastRequestTimestamp = System.currentTimeMillis()

        try {
            val urlString = "https://opencellid.org/cell/get?key=$apiKey&mcc=$mcc&mnc=$mnc&lac=$lac&cellid=$cellId&format=json"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "AntiFakeBTS-Android")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseText = reader.use { it.readText() }
                val json = JSONObject(responseText)

                val lat = json.optDouble("lat", Double.NaN)
                val lon = json.optDouble("lon", Double.NaN)
                val range = json.optInt("range", 0)

                val result = OpenCellIdResult(
                    isValidTower = !lat.isNaN() && !lon.isNaN(),
                    latitude = if (!lat.isNaN()) lat else null,
                    longitude = if (!lon.isNaN()) lon else null,
                    rangeMeters = range
                )
                memoryCache[cacheKey] = result
                result
            } else if (responseCode == 404) {
                val result = OpenCellIdResult(
                    isValidTower = false,
                    errorMessage = "Sel tidak terdaftar di database OpenCellID"
                )
                memoryCache[cacheKey] = result
                result
            } else {
                OpenCellIdResult(
                    isValidTower = false,
                    errorMessage = "Server OpenCellID merespons HTTP $responseCode"
                )
            }
        } catch (e: Exception) {
            OpenCellIdResult(
                isValidTower = false,
                errorMessage = "Gagal menghubungi OpenCellID: ${e.localizedMessage}"
            )
        }
    }
}
