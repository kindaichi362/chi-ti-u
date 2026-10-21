package com.example.ai

import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.Debt
import com.example.data.model.DebtType
import com.example.data.model.Goal
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.countsInReports
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/** A credit card's position this cycle. */
data class CreditCardStatus(
    val account: Account,
    val debt: Double,
    val limit: Double,
    val spentThisMonth: Double,
    /** Days until the next payment due date, or null when the due day is not set. */
    val daysToDue: Int?
) {
    val utilization: Double get() = if (limit > 0) debt / limit else 0.0
    val available: Double get() = (limit - debt).coerceAtLeast(0.0)
}

data class BudgetStatus(
    val budget: Budget,
    val categoryName: String,
    val emoji: String?,
    val spent: Double
) {
    val ratio: Double get() = if (budget.amountLimit > 0) spent / budget.amountLimit else 0.0
    val remaining: Double get() = budget.amountLimit - spent
}

enum class InsightTone { GOOD, INFO, WARNING, DANGER }

/** One short finding shown as a card in the assistant and on the home screen. */
data class Insight(val emoji: String, val title: String, val body: String, val tone: InsightTone, val followUp: String)

data class FinancialSnapshot(
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double,
    val monthlyIncome: Double,
    val monthlyExpense: Double,
    val monthlySavings: Double,
    val savingsRate: Double,
    val topExpenseCategory: Pair<String, Double>?,
    val categoryExpenses: Map<String, Double>,
    val activeBudgets: List<Budget>,
    val exceededBudgets: List<Pair<String, Double>>,
    val goals: List<Goal>,
    val debts: List<Debt>,
    // --- added for the analysis ---
    val previousMonthExpense: Double = 0.0,
    val groupExpenses: Map<String, Double> = emptyMap(),
    val previousGroupExpenses: Map<String, Double> = emptyMap(),
    val dayOfMonth: Int = 1,
    val daysInMonth: Int = 30,
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val creditCards: List<CreditCardStatus> = emptyList(),
    val largestExpenses: List<Transaction> = emptyList(),
    val categoryNames: Map<Long, String> = emptyMap()
) {
    val daysLeft: Int get() = (daysInMonth - dayOfMonth + 1).coerceAtLeast(1)
    val dailyAverage: Double get() = monthlyExpense / dayOfMonth.coerceAtLeast(1)
    /** Where spending lands at month end if the pace so far continues. */
    val projectedExpense: Double get() = dailyAverage * daysInMonth
    val totalBudget: Double get() = budgetStatuses.sumOf { it.budget.amountLimit }
    val totalBudgetSpent: Double get() = budgetStatuses.sumOf { it.spent }
    /** How much can still be spent per day without breaking the month's budgets. */
    val dailyAllowance: Double get() = ((totalBudget - totalBudgetSpent) / daysLeft).coerceAtLeast(0.0)
    val creditCardDebt: Double get() = creditCards.sumOf { it.debt }
    val creditCardLimit: Double get() = creditCards.sumOf { it.limit }
}

object FinancialAdvisorEngine {

    private val vnFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))

    fun formatVnd(amount: Double): String = com.example.ui.components.formatVnd(amount)

    /** "1,2 tr", "350k" — for sentences, where full numbers are hard to read. */
    fun compact(amount: Double): String {
        val a = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            a >= 1_000_000_000 -> sign + String.format(Locale.forLanguageTag("vi-VN"), "%.1f tỷ", a / 1_000_000_000).replace(",0 ", " ")
            a >= 1_000_000 -> sign + String.format(Locale.forLanguageTag("vi-VN"), "%.1f tr", a / 1_000_000).replace(",0 ", " ")
            a >= 1_000 -> sign + "${(a / 1_000).roundToInt()}k"
            else -> sign + "${a.toLong()}đ"
        }
    }

    private fun monthStart(cal: Calendar): Calendar = (cal.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    fun computeSnapshot(
        accounts: List<Account>,
        categories: List<Category>,
        transactionsThisMonth: List<Transaction>,
        budgets: List<Budget>,
        goals: List<Goal>,
        debts: List<Debt>,
        allTransactions: List<Transaction> = transactionsThisMonth,
        now: Long = System.currentTimeMillis()
    ): FinancialSnapshot {
        val catById = categories.associateBy { it.id }
        val totalAssets = accounts.filter { it.balance > 0 && !it.isExcludedFromNetWorth }.sumOf { it.balance }
        val totalLiabilities = abs(accounts.filter { it.balance < 0 && !it.isExcludedFromNetWorth }.sumOf { it.balance }) +
            debts.filter { it.type == DebtType.I_OWE && !it.isSettled }.sumOf { it.remainingAmount }

        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val thisStart = monthStart(cal).timeInMillis
        val prevStart = monthStart(cal).apply { add(Calendar.MONTH, -1) }.timeInMillis
        // Compare with the same number of days into last month, so day 10 is not compared with a full month.
        val prevSameDay = monthStart(cal).apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH).coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
            set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY)); set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
        }.timeInMillis

        val monthTx = transactionsThisMonth.filter { it.countsInReports }
        val prevTx = allTransactions.filter { it.countsInReports && it.type == TransactionType.EXPENSE && it.timestamp in prevStart until prevSameDay }

        val monthlyIncome = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expenses = monthTx.filter { it.type == TransactionType.EXPENSE }
        val monthlyExpense = expenses.sumOf { it.amount }

        fun nameOf(tx: Transaction) = catById[tx.categoryId]?.name ?: "Chưa phân loại"
        fun groupOf(tx: Transaction) = catById[tx.categoryId]?.groupName ?: "Khác"

        val categoryExpenseMap = expenses.groupBy(::nameOf).mapValues { (_, l) -> l.sumOf { it.amount } }
        val groupExpenses = expenses.groupBy(::groupOf).mapValues { (_, l) -> l.sumOf { it.amount } }
        val previousGroupExpenses = prevTx.groupBy(::groupOf).mapValues { (_, l) -> l.sumOf { it.amount } }

        val monthlySavings = (monthlyIncome - monthlyExpense).coerceAtLeast(0.0)
        val savingsRate = if (monthlyIncome > 0) monthlySavings / monthlyIncome * 100.0 else 0.0

        val spentByCategoryId = expenses.groupBy { it.categoryId }.mapValues { (_, l) -> l.sumOf { it.amount } }
        val budgetStatuses = budgets.map { b ->
            // A budget on a parent category also covers its sub-categories.
            val ids = setOf(b.categoryId) + categories.filter { it.parentId == b.categoryId }.map { it.id }
            BudgetStatus(b, catById[b.categoryId]?.name ?: "Danh mục", catById[b.categoryId]?.emoji, ids.sumOf { spentByCategoryId[it] ?: 0.0 })
        }.sortedByDescending { it.ratio }

        val creditCards = accounts.filter { it.type == AccountType.CREDIT_CARD }.map { card ->
            CreditCardStatus(
                account = card,
                debt = (-card.balance).coerceAtLeast(0.0),
                limit = card.creditLimit,
                spentThisMonth = expenses.filter { it.sourceAccountId == card.id }.sumOf { it.amount },
                daysToDue = card.dueDay?.let { daysUntilDay(it, now) }
            )
        }

        return FinancialSnapshot(
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            netWorth = totalAssets - totalLiabilities,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            monthlySavings = monthlySavings,
            savingsRate = savingsRate,
            topExpenseCategory = categoryExpenseMap.maxByOrNull { it.value }?.toPair(),
            categoryExpenses = categoryExpenseMap,
            activeBudgets = budgets,
            exceededBudgets = budgetStatuses.filter { it.spent > it.budget.amountLimit }.map { it.categoryName to it.spent - it.budget.amountLimit },
            goals = goals,
            debts = debts,
            previousMonthExpense = prevTx.sumOf { it.amount },
            groupExpenses = groupExpenses,
            previousGroupExpenses = previousGroupExpenses,
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
            daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH),
            budgetStatuses = budgetStatuses,
            creditCards = creditCards,
            largestExpenses = expenses.sortedByDescending { it.amount }.take(3),
            categoryNames = categories.associate { it.id to it.name }
        )
    }

    /** Days from [now] until the next occurrence of [day] of month (clamped to short months). */
    fun daysUntilDay(day: Int, now: Long = System.currentTimeMillis()): Int {
        val today = Calendar.getInstance().apply {
            timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val target = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH))) }
        if (target.before(today)) {
            target.set(Calendar.DAY_OF_MONTH, 1)
            target.add(Calendar.MONTH, 1)
            target.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, target.getActualMaximum(Calendar.DAY_OF_MONTH)))
        }
        return ((target.timeInMillis - today.timeInMillis) / 86_400_000L).toInt()
    }

    private fun pct(x: Double) = "${(x * 100).roundToInt()}%"

    /** "Thẻ X" without doubling when the account is already named "Thẻ …". */
    private fun cardName(card: CreditCardStatus): String =
        card.account.name.let { if (it.startsWith("thẻ", ignoreCase = true)) it else "Thẻ $it" }

    /** Findings worth telling the user right now, most urgent first. */
    fun insights(s: FinancialSnapshot): List<Insight> {
        val out = mutableListOf<Insight>()

        for (card in s.creditCards) {
            val due = card.daysToDue
            if (card.debt > 0 && due != null && due <= 5) {
                out += Insight("💳", "${cardName(card)} đến hạn ${if (due == 0) "hôm nay" else "sau $due ngày"}",
                    "Dư nợ ${compact(card.debt)}. Trả đủ trước hạn để không mất lãi (thường 25–35%/năm).", InsightTone.DANGER, "Thẻ tín dụng của tôi thế nào?")
            }
            if (card.limit > 0 && card.utilization >= 0.7) {
                out += Insight("⚠️", "${cardName(card)} đã dùng ${pct(card.utilization)} hạn mức",
                    "Còn trống ${compact(card.available)}. Giữ dưới 30% hạn mức tốt cho điểm tín dụng.", InsightTone.WARNING, "Thẻ tín dụng của tôi thế nào?")
            }
        }

        for (b in s.budgetStatuses.filter { it.ratio >= 0.9 }.take(2)) {
            val over = b.spent > b.budget.amountLimit
            out += Insight(b.emoji ?: "📊", if (over) "Vượt ngân sách ${b.categoryName}" else "${b.categoryName} sắp chạm ngân sách",
                if (over) "Đã chi ${compact(b.spent)}, vượt ${compact(-b.remaining)} so với ${compact(b.budget.amountLimit)}."
                else "Đã dùng ${pct(b.ratio)} hạn mức, còn ${compact(b.remaining)} cho ${s.daysLeft} ngày.",
                if (over) InsightTone.DANGER else InsightTone.WARNING, "Ngân sách tháng này còn bao nhiêu?")
        }

        if (s.totalBudget > 0 && s.projectedExpense > s.totalBudget * 1.05 && s.dayOfMonth >= 5) {
            out += Insight("📈", "Đang tiêu nhanh hơn kế hoạch",
                "Với tốc độ ${compact(s.dailyAverage)}/ngày, cuối tháng sẽ chi khoảng ${compact(s.projectedExpense)}, quá ngân sách ${compact(s.projectedExpense - s.totalBudget)}.",
                InsightTone.WARNING, "Tôi nên cắt giảm khoản nào?")
        }

        // The group that grew most compared with the same point last month.
        s.groupExpenses
            .mapNotNull { (g, v) -> s.previousGroupExpenses[g]?.takeIf { it >= 100_000 }?.let { prev -> Triple(g, v, (v - prev) / prev) } }
            .filter { it.third >= 0.3 && it.second - (s.previousGroupExpenses[it.first] ?: 0.0) >= 200_000 }
            .maxByOrNull { it.third }
            ?.let { (g, v, growth) ->
                out += Insight("🔺", "$g tăng ${pct(growth)} so với tháng trước",
                    "Tháng này ${compact(v)}, cùng kỳ tháng trước ${compact(s.previousGroupExpenses[g] ?: 0.0)}.", InsightTone.INFO, "So với tháng trước thế nào?")
            }

        if (s.monthlyIncome > 0) {
            out += if (s.savingsRate >= 20) Insight("🎉", "Tiết kiệm được ${pct(s.savingsRate / 100)} thu nhập",
                "Đã để dành ${compact(s.monthlySavings)} tháng này. Mức khuyến nghị là từ 20%.", InsightTone.GOOD, "Tháng này tôi tiết kiệm được bao nhiêu?")
            else Insight("🐷", "Tỷ lệ tiết kiệm mới ${pct(s.savingsRate / 100)}",
                "Thu ${compact(s.monthlyIncome)}, chi ${compact(s.monthlyExpense)}. Mục tiêu nên từ 20%.", InsightTone.INFO, "Tôi nên cắt giảm khoản nào?")
        }

        if (out.isEmpty() && s.monthlyExpense > 0) {
            out += Insight("✅", "Chi tiêu đang trong tầm kiểm soát",
                "Đã chi ${compact(s.monthlyExpense)}, trung bình ${compact(s.dailyAverage)}/ngày.", InsightTone.GOOD, "Tháng này tôi tiêu bao nhiêu tiền?")
        }
        return out
    }

    fun answerQuery(query: String, s: FinancialSnapshot): String {
        val q = VnText.unaccent(query)
        fun has(vararg words: String) = words.any { q.contains(VnText.unaccent(it)) }

        return when {
            has("the tin dung", "the credit", "du no", "han muc", "sao ke", "tra the") -> creditCardReport(s)
            has("cat giam", "tiet kiem hon", "nen bot", "goi y", "lam sao de") -> savingTips(s)
            has("thang truoc", "so voi", "so sanh", "tang hay giam") -> compareReport(s)
            has("ngan sach", "con duoc tieu", "moi ngay", "con bao nhieu") -> budgetReport(s)
            has("du bao", "cuoi thang") -> forecastReport(s)
            has("nhieu nhat", "lon nhat") -> topReport(s)
            has("tieu bao nhieu", "tong chi", "chi tieu thang") -> spendReport(s)
            has("tiet kiem", "de danh", "du duoc") -> savingsReport(s)
            has("qua tay", "vuot", "canh bao") -> warningReport(s)
            has("tai san", "co bao nhieu tien", "net worth") -> netWorthReport(s)
            has("o to", "mua xe", "mua nha") -> purchasePlan(q, s)
            else -> overview(s)
        }
    }

    private fun spendReport(s: FinancialSnapshot): String = buildString {
        append("📊 **Tháng này bạn đã chi ${formatVnd(s.monthlyExpense)}** (ngày ${s.dayOfMonth}/${s.daysInMonth}).\n")
        append("• Trung bình ${compact(s.dailyAverage)}/ngày, dự kiến cả tháng ~${compact(s.projectedExpense)}.\n")
        s.groupExpenses.entries.sortedByDescending { it.value }.take(3).forEach {
            append("• ${it.key}: ${compact(it.value)} (${pct(it.value / s.monthlyExpense.coerceAtLeast(1.0))})\n")
        }
    }.trim()

    private fun topReport(s: FinancialSnapshot): String {
        val top = s.topExpenseCategory ?: return "Bạn chưa có khoản chi nào trong tháng này."
        return buildString {
            append("🏆 Bạn chi nhiều nhất cho **${top.first}**: ${formatVnd(top.second)} (${pct(top.second / s.monthlyExpense.coerceAtLeast(1.0))} tổng chi).\n")
            if (s.largestExpenses.isNotEmpty()) {
                append("\n3 khoản lớn nhất:\n")
                s.largestExpenses.forEach { append("• ${it.note.ifBlank { s.categoryNames[it.categoryId] ?: "Không ghi chú" }}: ${compact(it.amount)}\n") }
            }
        }.trim()
    }

    private fun compareReport(s: FinancialSnapshot): String {
        if (s.previousMonthExpense <= 0) return "Chưa có dữ liệu tháng trước để so sánh. Ghi chép đủ một tháng là mình so được ngay."
        val diff = s.monthlyExpense - s.previousMonthExpense
        return buildString {
            append(if (diff > 0) "🔺 " else "🔻 ")
            append("Tính đến ngày ${s.dayOfMonth}, bạn chi **${compact(s.monthlyExpense)}**, ")
            append(if (diff > 0) "nhiều hơn" else "ít hơn")
            append(" cùng kỳ tháng trước **${compact(abs(diff))}** (${pct(abs(diff) / s.previousMonthExpense)}).\n\n")
            val groups = (s.groupExpenses.keys + s.previousGroupExpenses.keys).map {
                Triple(it, s.groupExpenses[it] ?: 0.0, s.previousGroupExpenses[it] ?: 0.0)
            }.sortedByDescending { abs(it.second - it.third) }.take(4)
            groups.forEach { (g, now, prev) -> append("• $g: ${compact(prev)} → ${compact(now)}\n") }
        }.trim()
    }

    private fun budgetReport(s: FinancialSnapshot): String {
        if (s.budgetStatuses.isEmpty()) return "Bạn chưa đặt ngân sách tháng này. Vào Báo cáo › Ngân sách để đặt hạn mức cho các nhóm chi chính, mình sẽ theo dõi giúp."
        return buildString {
            append("🎯 **Ngân sách tháng: đã dùng ${compact(s.totalBudgetSpent)} / ${compact(s.totalBudget)}.**\n")
            append("Còn ${s.daysLeft} ngày, mỗi ngày còn được tiêu khoảng **${compact(s.dailyAllowance)}**.\n\n")
            s.budgetStatuses.take(5).forEach {
                val mark = when { it.ratio > 1 -> "🔴"; it.ratio >= 0.9 -> "🟠"; it.ratio >= 0.7 -> "🟡"; else -> "🟢" }
                append("$mark ${it.categoryName}: ${compact(it.spent)} / ${compact(it.budget.amountLimit)} (${pct(it.ratio)})\n")
            }
        }.trim()
    }

    private fun forecastReport(s: FinancialSnapshot): String = buildString {
        append("🔮 Với tốc độ ${compact(s.dailyAverage)}/ngày, cuối tháng bạn sẽ chi khoảng **${compact(s.projectedExpense)}**.\n")
        if (s.totalBudget > 0) {
            val gap = s.projectedExpense - s.totalBudget
            append(if (gap > 0) "Vượt ngân sách khoảng ${compact(gap)} — cần giảm còn ${compact(s.dailyAllowance)}/ngày." else "Vẫn nằm trong ngân sách, dư khoảng ${compact(-gap)}.")
        }
        if (s.monthlyIncome > 0) append("\nThu nhập đã ghi: ${compact(s.monthlyIncome)}.")
    }

    private fun savingsReport(s: FinancialSnapshot): String =
        "💰 **Tiết kiệm tháng này:**\n• Thu: ${formatVnd(s.monthlyIncome)}\n• Chi: ${formatVnd(s.monthlyExpense)}\n" +
            "• Để dành được: **${formatVnd(s.monthlySavings)}** (${String.format(Locale.forLanguageTag("vi-VN"), "%.1f", s.savingsRate)}%)\n" +
            if (s.savingsRate >= 20) "🎉 Đạt chuẩn 50/30/20." else "Mục tiêu nên từ 20% thu nhập. Hỏi \"tôi nên cắt giảm khoản nào\" để xem gợi ý."

    private fun warningReport(s: FinancialSnapshot): String = when {
        s.exceededBudgets.isNotEmpty() ->
            "⚠️ **${s.exceededBudgets.size} ngân sách đã vượt:**\n" + s.exceededBudgets.joinToString("\n") { "• ${it.first}: vượt ${compact(it.second)}" }
        s.monthlyIncome > 0 && s.monthlyExpense > s.monthlyIncome ->
            "⚠️ Chi (${compact(s.monthlyExpense)}) đang lớn hơn thu (${compact(s.monthlyIncome)}) ${compact(s.monthlyExpense - s.monthlyIncome)}."
        s.creditCards.any { it.utilization >= 0.7 } -> creditCardReport(s)
        else -> "✅ Không có ngân sách nào vượt. Trung bình ${compact(s.dailyAverage)}/ngày."
    }

    private fun netWorthReport(s: FinancialSnapshot): String =
        "💎 **Tài sản ròng: ${formatVnd(s.netWorth)}**\n• Tài sản: ${formatVnd(s.totalAssets)}\n• Nợ (gồm thẻ tín dụng ${compact(s.creditCardDebt)}): ${formatVnd(s.totalLiabilities)}"

    private fun creditCardReport(s: FinancialSnapshot): String {
        if (s.creditCards.isEmpty()) return "Bạn chưa thêm thẻ tín dụng nào. Vào Tài khoản › Thêm › Thẻ tín dụng, nhập hạn mức và ngày đến hạn để mình nhắc trả thẻ và theo dõi hạn mức."
        return buildString {
            append("💳 **Thẻ tín dụng: dư nợ ${compact(s.creditCardDebt)}")
            if (s.creditCardLimit > 0) append(" / hạn mức ${compact(s.creditCardLimit)} (${pct(s.creditCardDebt / s.creditCardLimit)})")
            append("**\n\n")
            for (c in s.creditCards) {
                append("• **${c.account.name}**: nợ ${compact(c.debt)}")
                if (c.limit > 0) append(", dùng ${pct(c.utilization)} hạn mức")
                if (c.spentThisMonth > 0) append(", tháng này quẹt ${compact(c.spentThisMonth)}")
                c.daysToDue?.let { append(", đến hạn ${if (it == 0) "hôm nay" else "sau $it ngày"}") }
                append("\n")
            }
            append("\n")
            when {
                s.creditCards.any { it.debt > 0 && (it.daysToDue ?: 99) <= 5 } -> append("👉 Có thẻ sắp đến hạn: trả **toàn bộ** dư nợ sao kê để không bị tính lãi.")
                s.creditCards.any { it.utilization >= 0.3 } -> append("👉 Nên giữ dư nợ dưới 30% hạn mức; trả bớt giữa kỳ nếu cần.")
                else -> append("👍 Thẻ đang được dùng ở mức an toàn.")
            }
            if (s.creditCards.any { it.dueDayMissing() }) append("\n(Nhập ngày đến hạn cho thẻ để mình nhắc đúng lúc.)")
        }
    }

    private fun CreditCardStatus.dueDayMissing() = daysToDue == null

    private fun savingTips(s: FinancialSnapshot): String {
        if (s.monthlyExpense <= 0) return "Chưa có chi tiêu tháng này để phân tích."
        val discretionary = setOf("Ăn uống", "Mua sắm", "Giải trí", "Làm đẹp", "Du lịch")
        val candidates = s.groupExpenses.filterKeys { it in discretionary }.entries.sortedByDescending { it.value }.take(3)
        return buildString {
            append("✂️ **Gợi ý cắt giảm tháng này:**\n")
            candidates.forEach { append("• ${it.key}: ${compact(it.value)} — giảm 20% tiết kiệm ~${compact(it.value * 0.2)}\n") }
            val cardInterest = s.creditCards.filter { it.debt > 0 }
            if (cardInterest.isNotEmpty()) append("• Trả hết dư nợ thẻ ${compact(s.creditCardDebt)} đúng hạn để tránh lãi ~${compact(s.creditCardDebt * 0.025)}/tháng\n")
            val total = candidates.sumOf { it.value * 0.2 }
            if (total > 0) append("\n👉 Tổng có thể để dành thêm khoảng **${compact(total)}/tháng**.")
        }.trim()
    }

    private fun purchasePlan(q: String, s: FinancialSnapshot): String {
        val target = Regex("(\\d+)\\s*(ty|trieu|tr)").find(q)?.let { m ->
            val n = m.groupValues[1].toDouble()
            if (m.groupValues[2] == "ty") n * 1_000_000_000 else n * 1_000_000
        } ?: if (q.contains("nha")) 2_000_000_000.0 else 800_000_000.0
        val have = s.netWorth.coerceAtLeast(0.0)
        val perMonth = s.monthlySavings.takeIf { it > 0 } ?: return "Tháng này chưa có khoản để dành nào, nên chưa ước tính được thời gian tích luỹ cho ${compact(target)}."
        val months = ceil((target - have).coerceAtLeast(0.0) / perMonth).toInt()
        return "🏁 **Mục tiêu ${compact(target)}:** đang có ${compact(have)}, còn thiếu ${compact((target - have).coerceAtLeast(0.0))}.\n" +
            "Để dành ${compact(perMonth)}/tháng như tháng này thì cần khoảng **$months tháng** (${months / 12} năm ${months % 12} tháng)."
    }

    private fun overview(s: FinancialSnapshot): String = buildString {
        append("💡 **Tổng quan tháng này:** thu ${compact(s.monthlyIncome)}, chi ${compact(s.monthlyExpense)}, để dành ${compact(s.monthlySavings)}.\n")
        if (s.creditCards.isNotEmpty()) append("Thẻ tín dụng đang nợ ${compact(s.creditCardDebt)}.\n")
        insights(s).take(2).forEach { append("\n${it.emoji} ${it.title}. ${it.body}") }
        append("\n\nBạn có thể hỏi: \"so với tháng trước\", \"thẻ tín dụng\", \"còn được tiêu bao nhiêu\", \"nên cắt giảm khoản nào\".")
    }
}
