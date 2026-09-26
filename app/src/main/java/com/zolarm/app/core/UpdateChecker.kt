package com.zolarm.app.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.zolarm.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val TAG = "ZolarmUpdate"
    private const val API = "https://api.github.com/repos/azmifatahi/Zolarm/releases"
    private const val PAGE = "https://github.com/azmifatahi/Zolarm/releases"

    data class Result(
        val isLatest: Boolean,
        val current: String,
        val latest: String?,
        val apkUrl: String? = null,
        val apkFile: File? = null,
        val error: String? = null
    )

    suspend fun checkAndDownload(context: Context): Result = withContext(Dispatchers.IO) {
        val current = BuildConfig.VERSION_NAME
        try {
            val body = httpGet(API) ?: return@withContext Result(true, current, null, error = "network")
            val arr = JSONArray(body)
            if (arr.length() == 0) return@withContext Result(true, current, current)
            val latest = arr.getJSONObject(0)
            var tag = latest.optString("tag_name", current)
            if (tag.startsWith("v") || tag.startsWith("V")) tag = tag.drop(1)
            val isLatest = compare(current, tag) >= 0
            var apkUrl: String? = null
            val assets = latest.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val name = assets.getJSONObject(i).optString("name")
                    if (name.endsWith(".apk", true)) {
                        apkUrl = assets.getJSONObject(i).optString("browser_download_url")
                        break
                    }
                }
            }
            if (isLatest || apkUrl.isNullOrBlank()) {
                return@withContext Result(isLatest, current, tag, apkUrl)
            }
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val out = File(dir, "Zolarm-update.apk")
            if (out.exists()) out.delete()
            download(apkUrl!!, out)
            Result(false, current, tag, apkUrl, out)
        } catch (t: Throwable) {
            Log.e(TAG, "update failed", t)
            Result(true, current, null, error = t.message)
        }
    }

    fun install(context: Context, apk: File): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return false
            }
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", apk)
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        } catch (t: Throwable) {
            Log.e(TAG, "install failed", t)
            false
        }
    }

    private fun httpGet(url: String): String? {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000; readTimeout = 12000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Zolarm-Android")
        }
        return if (c.responseCode == 200) c.inputStream.bufferedReader().readText() else null
    }

    private fun download(url: String, dest: File) {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20000; readTimeout = 60000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Zolarm-Android")
            setRequestProperty("Accept", "application/octet-stream")
        }
        c.inputStream.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
        if (dest.length() < 50_000L) error("APK too small")
    }

    private fun compare(a: String, b: String): Int {
        val pa = a.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 }
        val pb = b.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 }
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val d = pa.getOrElse(i) { 0 } - pb.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return 0
    }
}
