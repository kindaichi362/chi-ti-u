package com.example.data.backup

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import java.util.TimeZone

object NightlyBackupScheduler {
    const val JOB_ID = 4101
    private const val NEXT_JOB_ID = 4102

    /** Không bao giờ làm văng app: lỗi lên lịch chỉ bỏ qua lượt tự sao lưu. */
    fun schedule(context: Context, afterRun: Boolean = false, completedId: Int = JOB_ID) {
        runCatching { scheduleOrThrow(context, afterRun, completedId) }
    }

    private fun scheduleOrThrow(context: Context, afterRun: Boolean, completedId: Int) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        if (VpsBackup(context).token.isBlank()) {
            scheduler.cancel(JOB_ID)
            scheduler.cancel(NEXT_JOB_ID)
            return
        }
        // Mở app / lưu mã / khởi động máy: đã có job đang chờ thì giữ nguyên.
        if (!afterRun && (scheduler.getPendingJob(JOB_ID) != null || scheduler.getPendingJob(NEXT_JOB_ID) != null)) return
        // Sau khi một job chạy xong: dùng ID còn lại, vì đặt lại đúng ID đang chạy sẽ làm hệ thống dừng job đó.
        val id = if (afterRun && completedId == JOB_ID) NEXT_JOB_ID else JOB_ID
        val current = System.currentTimeMillis()
        val (earliest, latest) = nextBackupWindow(current, TimeZone.getDefault(), afterRun)
        val job = JobInfo.Builder(id, ComponentName(context, NightlyBackupJob::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setMinimumLatency((earliest - current).coerceAtLeast(0))
            .setOverrideDeadline((latest - current).coerceAtLeast(0))
            .setBackoffCriteria(30 * 60 * 1000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setPersisted(true)
            .build()
        scheduler.schedule(job)
    }
}
