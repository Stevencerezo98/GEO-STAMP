package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class UpdateInfo(
    val currentVersionName: String,
    val currentVersionCode: Long,
    val latestVersionName: String,
    val latestVersionCode: Long,
    val isMandatory: Boolean,
    val releaseDate: String,
    val releaseNotes: List<String>,
    val downloadUrl: String
)

sealed interface UpdateCheckResult {
    data object Idle : UpdateCheckResult
    data object Checking : UpdateCheckResult
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

object UpdateChecker {

    fun getCurrentVersionInfo(context: Context): Pair<String, Long> {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            val versionName = packageInfo.versionName ?: "1.0"
            Pair(versionName, versionCode)
        } catch (e: Exception) {
            Pair("1.0", 1L)
        }
    }

    suspend fun checkUpdate(
        context: Context,
        simulateNewVersion: Boolean = false
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val (currentVersionName, currentVersionCode) = getCurrentVersionInfo(context)

        // Artificial brief delay to show smooth checking state
        delay(600)

        if (simulateNewVersion) {
            // Realistic new release demonstration for UI testing
            val simulatedLatestCode = currentVersionCode + 2
            val simulatedLatestName = "2.1.0"
            val notes = listOf(
                "Nuevo sistema inteligente de Modo Oscuro con detección automática del sistema.",
                "Mejora en la nitidez de exportación de reportes PDF a resolución Ultra HD.",
                "Optimización en la velocidad de adquisición GPS y geocodificación.",
                "Nuevas opciones de filtrado y edición rápida de notas de campo."
            )
            val updateInfo = UpdateInfo(
                currentVersionName = currentVersionName,
                currentVersionCode = currentVersionCode,
                latestVersionName = simulatedLatestName,
                latestVersionCode = simulatedLatestCode,
                isMandatory = false,
                releaseDate = "08/11/2026",
                releaseNotes = notes,
                downloadUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
            )
            return@withContext UpdateCheckResult.UpdateAvailable(updateInfo)
        }

        // Production check: In production, this can query a remote JSON manifest (e.g., GitHub Releases / AWS / Firebase Remote Config)
        // Default: Up to date
        UpdateCheckResult.UpToDate(currentVersionName)
    }

    fun openUpdateUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser search
            val fallbackIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }
}
