package com.wishesapp.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.wishesapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val latestVersionName: String,
    val downloadUrl: String,
    val isUpdateAvailable: Boolean,
)

@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // Replace YOUR_USERNAME/WishesApp with the real GitHub username/repo once created
    private val versionJsonUrl =
        "https://YOUR_USERNAME.github.io/WishesApp/version.json"

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = java.net.URL(versionJsonUrl).openConnection()
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            val json = connection.getInputStream().bufferedReader().readText()
            val obj = JSONObject(json)
            val latestName = obj.getString("versionName")
            val downloadUrl = obj.getString("downloadUrl")
            val current = BuildConfig.VERSION_NAME
            // Compare semantic versions: "1.0.2" > "1.0.1"
            val isNewer = compareVersions(latestName, current) > 0
            UpdateInfo(latestName, downloadUrl, isNewer)
        } catch (e: Exception) {
            null // silently ignore network errors
        }
    }

    fun openDownloadUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun compareVersions(a: String, b: String): Int {
        val aParts = a.split(".").map { it.toIntOrNull() ?: 0 }
        val bParts = b.split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(aParts.size, bParts.size)
        for (i in 0 until len) {
            val diff = (aParts.getOrElse(i) { 0 }) - (bParts.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }
}
