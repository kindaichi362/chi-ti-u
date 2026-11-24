package com.example.data.backup

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

/**
 * Bản cài riêng của chủ máy có thể kèm file assets/vps_provision.txt (KHÔNG có trong bản công khai).
 * Lần đầu mở app, nếu chưa có mã kết nối thì lấy mã từ file đó để khỏi phải nhập tay.
 */
object VpsProvision {
    private const val ASSET = "vps_provision.txt"
    const val SOON_JOB_ID = 4103

    fun loadIfMissing(context: Context) = runCatching {
        val backup = VpsBackup(context)
        if (backup.token.isNotBlank()) return@runCatching
        val code = runCatching { context.assets.open(ASSET).bufferedReader().use { it.readText() }.trim() }.getOrNull()
        if (code != null && code.length == 64 && code.all { it in "0123456789abcdef" }) backup.token = code
    }

    /** Đẩy bản sao lưu lên VPS ~1 phút sau khi rời app (khi có mạng); job mới thay job cũ nên không chồng nhau. */
    fun backupSoon(context: Context) = runCatching {
        if (VpsBackup(context).token.isBlank()) return@runCatching
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.schedule(
            JobInfo.Builder(SOON_JOB_ID, ComponentName(context, NightlyBackupJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(60_000L)
                .setBackoffCriteria(5 * 60 * 1000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .build()
        )
    }
}
