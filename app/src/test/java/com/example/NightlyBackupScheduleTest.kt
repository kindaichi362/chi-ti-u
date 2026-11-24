package com.example

import com.example.data.backup.nextBackupWindow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NightlyBackupScheduleTest {
    private val zone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")

    private fun at(day: Int, hour: Int, minute: Int = 0): Long = Calendar.getInstance(zone).apply {
        clear()
        set(2026, Calendar.NOVEMBER, day, hour, minute)
    }.timeInMillis

    @Test fun beforeOne() = check(20, 0, 59, 20)
    @Test fun insideWindow() = check(20, 3, 0, 20)
    @Test fun afterFive() = check(20, 5, 0, 21)
    @Test fun acrossMidnight() = check(20, 23, 59, 21)
    @Test fun endOfMonth() = check(30, 23, 59, 1, Calendar.DECEMBER)

    private fun check(day: Int, hour: Int, minute: Int, expectedDay: Int, month: Int = Calendar.NOVEMBER) {
        val (start, end) = nextBackupWindow(at(day, hour, minute), zone)
        val expected = Calendar.getInstance(zone).apply { clear(); set(2026, month, expectedDay, 1, 0) }
        assertEquals(expected.timeInMillis, start)
        expected.set(Calendar.HOUR_OF_DAY, 5)
        assertEquals(expected.timeInMillis, end)
    }

    @Test fun afterRunSchedulesTomorrow() {
        assertEquals(at(21, 1), nextBackupWindow(at(20, 2), zone, afterRun = true).first)
    }
}
