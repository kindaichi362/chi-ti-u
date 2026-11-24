package com.example.data.backup

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive

class NightlyBackupJob : JobService() {
    private var work: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        work = CoroutineScope(Dispatchers.IO).launch {
            var retry = false
            try {
                val backup = VpsBackup(applicationContext)
                if (backup.token.isNotBlank()) {
                    backup.upload()
                    record("Thành công")
                }
            } catch (e: CancellationException) {
                return@launch
            } catch (e: Exception) {
                retry = e is java.io.IOException
                // Thông điệp của upload() chỉ có mã HTTP, không chứa token.
                record(if (retry) "Lỗi mạng, sẽ thử lại" else "Lỗi sao lưu: ${e.message ?: e.javaClass.simpleName}".take(120))
            } finally {
                // Dùng isActive của chính coroutine: `work` có thể chưa được gán nếu job kết thúc ngay.
                if (isActive) {
                    // Lỗi mạng: để hệ thống thử lại chính job này; chỉ lên lịch đêm sau khi không cần thử lại,
                    // tránh hai job cùng tồn tại và sao lưu hai lần một đêm.
                    // Lượt "sao lưu sau khi rời app" chỉ cần bảo đảm lịch đêm còn đó, không dời lịch đêm.
                    if (!retry) {
                        if (params.jobId == VpsProvision.SOON_JOB_ID) NightlyBackupScheduler.schedule(applicationContext)
                        else NightlyBackupScheduler.schedule(applicationContext, afterRun = true, completedId = params.jobId)
                    }
                    jobFinished(params, retry)
                }
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        work?.cancel()
        return true
    }

    private fun record(result: String) {
        getSharedPreferences("vps_backup", MODE_PRIVATE).edit()
            .putLong("last_nightly_backup_at", System.currentTimeMillis())
            .putString("last_nightly_backup_result", result)
            .apply()
    }
}
