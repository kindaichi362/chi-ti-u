package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Budget
import com.example.data.model.Goal
import com.example.data.model.PaymentStatus
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.dragon.data.DragonPet
import com.example.dragon.data.DragonRepository
import com.example.dragon.domain.CareRules
import com.example.dragon.domain.LevelCurve
import com.example.dragon.domain.MissionEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DragonTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase
    private lateinit var dragon: DragonRepository

    /** Wednesday 28 Oct 2026, 20:00 local time. */
    private val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 28, 20, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    private val day = 86_400_000L

    private fun tx(at: Long, type: TransactionType = TransactionType.EXPENSE, amount: Double = 50_000.0, category: Long = 1,
                   status: PaymentStatus = PaymentStatus.PAID) =
        Transaction(type = type, amount = amount, sourceAccountId = 1, categoryId = category, timestamp = at, paymentStatus = status)

    private fun input(txs: List<Transaction> = emptyList(), budgets: List<Budget> = emptyList(), goals: List<Goal> = emptyList(), at: Long = now) =
        MissionEngine.Input(txs, budgets, goals, at)

    @Before fun open() {
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        db = AppDatabase.getDatabase(app)
        dragon = DragonRepository(db)
    }

    @After fun close() = AppDatabase.resetForTests()

    @Test fun levelCurveMatchesSpec() {
        assertEquals(100L, LevelCurve.xpToNext(1))
        assertEquals(685L, LevelCurve.xpToNext(10))
        assertEquals(12_580L, LevelCurve.xpToNext(49))
        assertEquals(1, LevelCurve.progress(99).level)
        assertEquals(2, LevelCurve.progress(100).level)
        assertEquals(0L, LevelCurve.progress(100).xpInLevel)
        assertEquals(LevelCurve.MAX_LEVEL, LevelCurve.progress(LevelCurve.xpForLevel(50)).level)
        assertEquals(LevelCurve.MAX_LEVEL, LevelCurve.progress(Long.MAX_VALUE / 2).level)
        assertEquals(LevelCurve.Stage.NEWBORN, LevelCurve.stageOf(5))
        assertEquals(LevelCurve.Stage.TODDLER, LevelCurve.stageOf(6))
        assertEquals(LevelCurve.Stage.LEGEND, LevelCurve.stageOf(50))
    }

    @Test fun dailyMissionIsGrantedOncePerDay() = runBlocking {
        val today = input(listOf(tx(now - 3_600_000), tx(now - 7_200_000)))
        val first = dragon.reconcile(today)
        assertEquals(listOf("Ghi khoản đầu tiên hôm nay"), first.map { it.title })
        // Saving more transactions, reopening the app, reconciling again: nothing more.
        repeat(5) { assertTrue(dragon.reconcile(today).isEmpty()) }
        assertEquals(20L, db.dragonDao().totalXpOnce())
        assertEquals(5L, db.dragonDao().coinsOnce())
        // Next day it is due again.
        assertEquals(1, dragon.reconcile(input(listOf(tx(now + day)), at = now + day)).size)
    }

    @Test fun concurrentGrantsWriteOnlyOnce() = runBlocking {
        val results = (1..20).map { async { dragon.grant("goal:7", "MISSION", "x", 150, 80) } }.awaitAll()
        assertEquals(1, results.count { it != null })
        assertEquals(80L, db.dragonDao().coinsOnce())
    }

    @Test fun pendingAndAdjustmentsDoNotCount() {
        val missions = MissionEngine.evaluate(input(listOf(
            tx(now, status = PaymentStatus.PENDING_PAYMENT), tx(now, type = TransactionType.ADJUSTMENT)
        )), emptySet())
        assertEquals(MissionEngine.State.IN_PROGRESS, missions.first { it.key.startsWith("first_tx:") }.state)
    }

    @Test fun closingTheDayNeedsARecordAndIsManual() = runBlocking {
        val key = "close_day:" + MissionEngine.dayKey(now)
        assertNull(dragon.claim(input(), key))
        val withTx = input(listOf(tx(now)))
        dragon.reconcile(withTx)
        assertFalse(db.dragonDao().keysWithPrefix("close_day").any())
        assertEquals(30, dragon.claim(withTx, key)!!.xp)
        assertNull(dragon.claim(withTx, key))
    }

    @Test fun streakRewardsEverySevenDays() = runBlocking {
        val sevenDays = (0 until 7).map { tx(now - it * day) }
        assertEquals(7, MissionEngine.currentStreak(sevenDays, now))
        val grants = dragon.reconcile(input(sevenDays))
        assertTrue(grants.any { it.title == "Chuỗi 7 ngày ghi chép" })
        // Day 8 of the same run: no second 7-day reward.
        val eight = sevenDays + tx(now + day)
        assertFalse(dragon.reconcile(input(eight, at = now + day)).any { it.title.startsWith("Chuỗi") })
        // Six days: none.
        assertEquals(6, MissionEngine.currentStreak((0 until 6).map { tx(now - it * day) }, now))
        // A gap breaks it; yesterday still keeps the run alive in the display.
        assertEquals(2, MissionEngine.currentStreak(listOf(tx(now - day), tx(now - 2 * day), tx(now - 4 * day)), now))
    }

    @Test fun goalsBelowMinimumGiveNothing() {
        val small = Goal(id = 1, name = "Nhỏ", targetAmount = 100_000.0, currentAmount = 100_000.0)
        val big = Goal(id = 2, name = "Xe máy", targetAmount = 20_000_000.0, currentAmount = 20_000_000.0)
        val ready = MissionEngine.evaluate(input(goals = listOf(small, big)), emptySet()).filter { it.state == MissionEngine.State.READY }
        assertEquals(listOf("goal:2"), ready.map { it.key })
    }

    @Test fun weekUnderBudget() {
        val lastMonday = MissionEngine.weekStart(now) - 7 * day
        val budget = Budget(categoryId = 1, amountLimit = 300_000.0, monthYear = "2026-10")
        val within = (0 until 3).map { tx(lastMonday + it * day + 3_600_000) }
        val key = "under_budget:" + MissionEngine.weekKey(lastMonday)
        fun state(txs: List<Transaction>, b: List<Budget>) = MissionEngine.evaluate(input(txs, b), emptySet()).first { it.key == key }.state
        assertEquals(MissionEngine.State.READY, state(within, listOf(budget)))
        assertEquals(MissionEngine.State.IN_PROGRESS, state(within + tx(lastMonday + 5_000_000, amount = 500_000.0), listOf(budget)))
        assertEquals(MissionEngine.State.IN_PROGRESS, state(within.take(2), listOf(budget)))
        assertEquals(MissionEngine.State.IN_PROGRESS, state(within, emptyList()))
    }

    @Test fun careStatsNeverHitZeroAndXpIsCapped() = runBlocking {
        val tired = CareRules.decayed(DragonPet(energy = 90.0, happiness = 90.0, statsAt = now), now + 30 * day)
        assertEquals(CareRules.ENERGY_FLOOR, tired.energy, 0.0)
        assertEquals(CareRules.HAPPINESS_FLOOR, tired.happiness, 0.0)
        dragon.ensurePet()
        val grants = (1..9).map { dragon.care(CareRules.Action.PET, now + it) }
        assertEquals(CareRules.CARE_XP_PER_DAY, grants.count { it != null })
        assertEquals(CareRules.CARE_XP_PER_DAY.toLong(), db.dragonDao().totalXpOnce())
        assertEquals(100.0, db.dragonDao().petOnce()!!.happiness, 0.0)
    }

    @Test fun levelUpIsReported() = runBlocking {
        dragon.grant("a", "MISSION", "a", 90, 0)
        val g = dragon.grant("b", "MISSION", "b", 20, 0)!!
        assertTrue(g.leveledUp)
        assertEquals(2, g.levelAfter)
    }

    @Test fun reportRewardOncePerWeek() = runBlocking {
        assertEquals(20, dragon.reportViewed(now)!!.xp)
        assertNull(dragon.reportViewed(now + 3_600_000))
        assertTrue(dragon.reportViewed(now + 7 * day) != null)
    }
}
