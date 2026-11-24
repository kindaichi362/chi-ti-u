package com.example.data.backup

import java.util.Calendar
import java.util.TimeZone

/** Returns start and end of the next local 01:00–05:00 window in epoch milliseconds. */
internal fun nextBackupWindow(now: Long, zone: TimeZone, afterRun: Boolean = false): Pair<Long, Long> {
    val start = Calendar.getInstance(zone).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val end = (start.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 5) }
    if (afterRun || now >= end.timeInMillis) {
        start.add(Calendar.DAY_OF_MONTH, 1)
        end.add(Calendar.DAY_OF_MONTH, 1)
    }
    return start.timeInMillis to end.timeInMillis
}
