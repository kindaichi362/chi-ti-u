package com.example.data.backup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NightlyBackupBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) NightlyBackupScheduler.schedule(context)
    }
}
