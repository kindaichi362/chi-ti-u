package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class AvailableUpdate(val versionCode: Int, val versionName: String, val apkUrl: String, val sha256: String)

/** HTTPS version check and verified APK handoff to Android's package installer. */
class AppUpdate(private val context: Context) {
    suspend fun check(): AvailableUpdate? = withContext(Dispatchers.IO) {
        val conn = URL(MANIFEST_URL).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.instanceFollowRedirects = false
            conn.useCaches = false
            conn.setRequestProperty("Cache-Control", "no-cache")
            if (conn.responseCode != 200) error("Không đọc được phiên bản mới")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val versionCode = json.getInt("versionCode")
            val apkUrl = json.getString("apkUrl")
            val digest = json.getString("sha256").lowercase()
            require(URL(apkUrl).protocol == "https" && URL(apkUrl).host == DOWNLOAD_HOST) { "Địa chỉ tải không hợp lệ" }
            require(digest.matches(Regex("[0-9a-f]{64}"))) { "Mã kiểm tra tệp không hợp lệ" }
            if (versionCode > BuildConfig.VERSION_CODE) {
                AvailableUpdate(versionCode, json.getString("versionName"), apkUrl, digest)
            } else null
        } finally { conn.disconnect() }
    }

    suspend fun download(update: AvailableUpdate): File = withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(folder, "chi-tieu-${update.versionCode}.apk")
        if (target.exists() && sha256(target) == update.sha256) return@withContext target
        val partial = File(folder, "download.tmp")
        val conn = URL(update.apkUrl).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = false
            if (conn.responseCode != 200) error("Không tải được bản cập nhật")
            val length = conn.contentLengthLong
            require(length in 1..MAX_APK_BYTES) { "Kích thước bản cập nhật không hợp lệ" }
            conn.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_APK_BYTES) { "Bản cập nhật quá lớn" }
                        output.write(buffer, 0, count)
                    }
                }
            }
            require(sha256(partial) == update.sha256) { "Tệp tải xuống không khớp mã kiểm tra" }
            if (target.exists()) target.delete()
            require(partial.renameTo(target)) { "Không lưu được bản cập nhật" }
            target
        } finally {
            conn.disconnect()
            partial.delete()
        }
    }

    fun install(apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return true
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val DOWNLOAD_HOST = "download.180-93-125-5.sslip.io"
        private const val MANIFEST_URL = "https://$DOWNLOAD_HOST/latest.json"
        private const val MAX_APK_BYTES = 80L * 1024 * 1024
    }
}
