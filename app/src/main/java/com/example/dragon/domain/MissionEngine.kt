package com.example.dragon.domain

import com.example.data.model.Budget
import com.example.data.model.Goal
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.countsInReports
import java.util.Calendar
import java.util.Locale

/**
 * Turns finance data into missions. Pure: same input, same answer — no clock or database inside.
 *
 * Every mission has a ledger key that names its period ("first_tx:2026-10-28"); a reward is due
 * when the condition holds and that key is not in the ledger yet. Rewards follow recording habits,
 * never how much money moved, so the game cannot push anyone to spend.
 */
object MissionEngine {

    enum class State { DONE, READY, CLAIMABLE, IN_PROGRESS }

    data class Mission(
        val key: String,
        val title: String,
        val detail: String,
        val xp: Int,
        val coins: Int,
        val state: State,
        val progress: Float? = null,
        val period: String
    )

    data class Input(
        val transactions: List<Transaction>,
        val budgets: List<Budget>,
        val goals: List<Goal>,
        val now: Long
    )

    const val GOAL_MIN_TARGET = 500_000.0
    private const val DAY_MS = 86_400_000L

    fun evaluate(input: Input, granted: Set<String>): List<Mission> {
        val today = dayKey(input.now)
        val recorded = input.transactions.filter { it.countsInReports && it.type != TransactionType.ADJUSTMENT }
        val days = recorded.mapTo(HashSet()) { dayKey(it.timestamp) }
        val todayCount = recorded.count { dayKey(it.timestamp) == today }
        val result = mutableListOf<Mission>()

        fun add(key: String, title: String, detail: String, xp: Int, coins: Int, met: Boolean, period: String,
                progress: Float? = null, manual: Boolean = false) {
            val state = when {
                key in granted -> State.DONE
                met -> if (manual) State.CLAIMABLE else State.READY
                else -> State.IN_PROGRESS
            }
            result += Mission(key, title, detail, xp, coins, state, if (state == State.DONE) 1f else progress, period)
        }

        // ---- Daily ----
        add("first_tx:$today", "Ghi khoản đầu tiên hôm nay", "Ghi một khoản thu, chi hoặc chuyển tiền.",
            20, 5, todayCount > 0, "Hôm nay", if (todayCount > 0) 1f else 0f)
        add("close_day:$today", "Chốt sổ hôm nay", "Ghi xong các khoản trong ngày rồi bấm chốt.",
            30, 10, todayCount > 0, "Hôm nay", if (todayCount > 0) 1f else 0f, manual = true)

        // ---- Streak: one reward each time a run of consecutive recorded days reaches 7, 14, 21… ----
        val streak = streakEndingAt(days, input.now)
        val alive = if (streak > 0) streak else streakEndingAt(days, input.now - DAY_MS)
        if (streak >= 7) {
            val start = dayKey(input.now - (streak - 1) * DAY_MS)
            for (k in 1..streak / 7) {
                val key = "streak7:$start:$k"
                if (key !in granted) {
                    add(key, "Chuỗi ${7 * k} ngày ghi chép", "Ngày nào cũng ghi ít nhất một khoản.", 100, 50, true, "Chuỗi")
                }
            }
        }
        val nextTarget = (alive / 7 + 1) * 7
        add("streak_next", "Giữ chuỗi $nextTarget ngày", "Chuỗi hiện tại: $alive ngày.", 100, 50, false, "Chuỗi",
            (alive % 7) / 7f)

        // ---- Weekly ----
        val week = weekKey(input.now)
        add("report:$week", "Xem báo cáo tuần này", "Mở tab Báo cáo để xem tiền đi đâu.", 20, 10, false, "Tuần này", 0f)
        val lastWeekStart = weekStart(input.now) - 7 * DAY_MS
        val lastWeek = weekKey(lastWeekStart)
        val underBudget = weekUnderBudget(recorded, input.budgets, lastWeekStart)
        add("under_budget:$lastWeek", "Tuần trước không vượt ngân sách",
            "Ghi ít nhất 3 ngày trong tuần và mọi ngân sách tháng còn trong hạn mức.",
            60, 30, underBudget == true, "Tuần trước", if (underBudget == true) 1f else 0f)

        // ---- Once ----
        add("budget_once", "Đặt ngân sách đầu tiên", "Vào Báo cáo → Ngân sách, đặt hạn mức cho một danh mục.",
            50, 30, input.budgets.isNotEmpty(), "Một lần", if (input.budgets.isNotEmpty()) 1f else 0f)

        // ---- Savings goals (target ≥ 500k so tiny goals cannot be farmed) ----
        val eligible = input.goals.filter { it.targetAmount >= GOAL_MIN_TARGET }
        val reached = eligible.filter { it.currentAmount >= it.targetAmount }
        reached.forEach { goal ->
            add("goal:${goal.id}", "Hoàn thành mục tiêu “${goal.name}”", "Mục tiêu tiết kiệm từ 500.000 ₫.",
                150, 80, true, "Mục tiêu", 1f)
        }
        if (reached.all { "goal:${it.id}" in granted }) {
            val closest = eligible.filter { it.currentAmount < it.targetAmount }.maxByOrNull { it.currentAmount / it.targetAmount }
            add("goal_next", closest?.let { "Hoàn thành mục tiêu “${it.name}”" } ?: "Hoàn thành một mục tiêu tiết kiệm",
                "Mục tiêu tiết kiệm từ 500.000 ₫.", 150, 80, false, "Mục tiêu",
                closest?.let { (it.currentAmount / it.targetAmount).toFloat().coerceIn(0f, 1f) } ?: 0f)
        }
        return result
    }

    /** Consecutive recorded days ending on the day of [at]. */
    fun streakEndingAt(days: Set<String>, at: Long): Int {
        var n = 0
        val cal = Calendar.getInstance().apply { timeInMillis = at }
        while (dayKey(cal.timeInMillis) in days) {
            n++
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return n
    }

    fun currentStreak(transactions: List<Transaction>, now: Long): Int {
        val days = transactions.filter { it.countsInReports && it.type != TransactionType.ADJUSTMENT }.mapTo(HashSet()) { dayKey(it.timestamp) }
        return streakEndingAt(days, now).takeIf { it > 0 } ?: streakEndingAt(days, now - DAY_MS)
    }

    /**
     * Null when there is nothing to judge (no budgets for that month). Otherwise true when the
     * user recorded on at least 3 days of the week and every budget of the month the week ends in
     * was still within its limit at the end of the week.
     */
    internal fun weekUnderBudget(recorded: List<Transaction>, budgets: List<Budget>, weekStart: Long): Boolean? {
        val weekEnd = weekStart + 7 * DAY_MS
        val month = monthKey(weekEnd - 1)
        val monthBudgets = budgets.filter { it.monthYear == month }
        if (monthBudgets.isEmpty()) return null
        val activeDays = recorded.filter { it.timestamp in weekStart until weekEnd }.mapTo(HashSet()) { dayKey(it.timestamp) }
        if (activeDays.size < 3) return false
        val monthStart = Calendar.getInstance().apply {
            timeInMillis = weekEnd - 1; set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return monthBudgets.all { budget ->
            recorded.filter { it.type == TransactionType.EXPENSE && it.categoryId == budget.categoryId && it.timestamp in monthStart until weekEnd }
                .sumOf { it.amount } <= budget.amountLimit
        }
    }

    fun dayKey(t: Long): String = Calendar.getInstance().run {
        timeInMillis = t
        String.format(Locale.US, "%04d-%02d-%02d", get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH))
    }

    private fun monthKey(t: Long): String = dayKey(t).substring(0, 7)

    /** Midnight of the Monday of the week containing [t]. */
    fun weekStart(t: Long): Long = Calendar.getInstance().run {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        val back = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_MONTH, -back)
        timeInMillis
    }

    /** "W2026-10-26": the Monday the week starts on. */
    fun weekKey(t: Long): String = "W" + dayKey(weekStart(t))
}
