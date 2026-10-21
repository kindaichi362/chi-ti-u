package com.example.ai

import com.example.data.model.Account
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.Debt
import com.example.data.model.Goal
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import java.text.NumberFormat
import java.util.Locale

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
    val debts: List<Debt>
)

object FinancialAdvisorEngine {

    private val vnFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    fun formatVnd(amount: Double): String {
        return "${vnFormat.format(amount.toLong())} ₫"
    }

    fun computeSnapshot(
        accounts: List<Account>,
        categories: List<Category>,
        transactionsThisMonth: List<Transaction>,
        budgets: List<Budget>,
        goals: List<Goal>,
        debts: List<Debt>
    ): FinancialSnapshot {
        val totalAssets = accounts.filter { it.balance > 0 && !it.isExcludedFromNetWorth }.sumOf { it.balance }
        val totalLiabilities = kotlin.math.abs(accounts.filter { it.balance < 0 && !it.isExcludedFromNetWorth }.sumOf { it.balance }) +
                debts.filter { it.type == com.example.data.model.DebtType.I_OWE && !it.isSettled }.sumOf { it.remainingAmount }
        val netWorth = totalAssets - totalLiabilities

        var monthlyIncome = 0.0
        var monthlyExpense = 0.0
        val categoryExpenseMap = mutableMapOf<String, Double>()

        for (tx in transactionsThisMonth) {
            when (tx.type) {
                TransactionType.INCOME -> monthlyIncome += tx.amount
                TransactionType.EXPENSE -> {
                    monthlyExpense += tx.amount
                    val catName = categories.firstOrNull { it.id == tx.categoryId }?.name ?: "Khác"
                    categoryExpenseMap[catName] = (categoryExpenseMap[catName] ?: 0.0) + tx.amount
                }
                else -> {}
            }
        }

        val monthlySavings = (monthlyIncome - monthlyExpense).coerceAtLeast(0.0)
        val savingsRate = if (monthlyIncome > 0) (monthlySavings / monthlyIncome) * 100.0 else 0.0

        val topCategory = categoryExpenseMap.maxByOrNull { it.value }?.toPair()

        val exceededBudgets = mutableListOf<Pair<String, Double>>()
        for (b in budgets) {
            val cat = categories.firstOrNull { it.id == b.categoryId }
            val spent = categoryExpenseMap[cat?.name] ?: 0.0
            if (spent > b.amountLimit) {
                exceededBudgets.add(Pair(cat?.name ?: "Danh mục", spent - b.amountLimit))
            }
        }

        return FinancialSnapshot(
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            netWorth = netWorth,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            monthlySavings = monthlySavings,
            savingsRate = savingsRate,
            topExpenseCategory = topCategory,
            categoryExpenses = categoryExpenseMap,
            activeBudgets = budgets,
            exceededBudgets = exceededBudgets,
            goals = goals,
            debts = debts
        )
    }

    fun answerQuery(query: String, snapshot: FinancialSnapshot): String {
        val q = query.lowercase(Locale.ROOT)

        return when {
            q.contains("tiêu bao nhiêu") || q.contains("tổng chi") || q.contains("chi tiêu tháng") -> {
                "📊 **Tổng chi tiêu tháng này của bạn là:** ${formatVnd(snapshot.monthlyExpense)}.\n\n" +
                if (snapshot.topExpenseCategory != null) {
                    "Trong đó, khoản chi lớn nhất là **${snapshot.topExpenseCategory.first}** với ${formatVnd(snapshot.topExpenseCategory.second)}."
                } else "Bạn chưa có khoản chi lớn nào được ghi nhận."
            }

            q.contains("tiêu nhiều nhất") || q.contains("khoản nào lớn nhất") || q.contains("chi nhiều nhất") -> {
                if (snapshot.topExpenseCategory != null) {
                    "🏆 Khoản bạn chi nhiều nhất trong tháng là **${snapshot.topExpenseCategory.first}** với tổng số tiền **${formatVnd(snapshot.topExpenseCategory.second)}** " +
                    "(${if (snapshot.monthlyExpense > 0) String.format("%.1f", (snapshot.topExpenseCategory.second / snapshot.monthlyExpense) * 100) else "0"}% tổng chi)."
                } else {
                    "Bạn chưa có giao dịch chi tiêu nào trong tháng này."
                }
            }

            q.contains("tiết kiệm") || q.contains("để dành") || q.contains("dư được bao nhiêu") -> {
                val rateStr = String.format("%.1f", snapshot.savingsRate)
                "💰 **Tình hình tiết kiệm tháng này:**\n" +
                "• Thu nhập: ${formatVnd(snapshot.monthlyIncome)}\n" +
                "• Chi tiêu: ${formatVnd(snapshot.monthlyExpense)}\n" +
                "• Tiết kiệm ròng: **${formatVnd(snapshot.monthlySavings)}**\n" +
                "• Tỷ lệ tiết kiệm: **$rateStr%** " +
                if (snapshot.savingsRate >= 20) "🎉 (Rất tốt! Bạn đang đạt chuẩn tài chính 50/30/20)." else "⚠️ (Hơi thấp, bạn nên tối ưu lại chi tiêu không thiết yếu)."
            }

            q.contains("quá tay") || q.contains("vượt ngân sách") || q.contains("cảnh báo") -> {
                if (snapshot.exceededBudgets.isNotEmpty()) {
                    val list = snapshot.exceededBudgets.joinToString("\n") { "• **${it.first}**: Vượt ${formatVnd(it.second)}" }
                    "⚠️ **Cảnh báo vượt ngân sách:**\nBạn có ${snapshot.exceededBudgets.size} danh mục đã vượt hạn mức:\n$list\n\nBạn nên cân nhắc hãm phanh chi tiêu cho các khoản này từ nay đến cuối tháng nhé!"
                } else if (snapshot.monthlyExpense > snapshot.monthlyIncome && snapshot.monthlyIncome > 0) {
                    "⚠️ Bạn đang có dấu hiệu tiêu quá tay! Chi tiêu (${formatVnd(snapshot.monthlyExpense)}) đã vượt thu nhập (${formatVnd(snapshot.monthlyIncome)}) khoảng ${formatVnd(snapshot.monthlyExpense - snapshot.monthlyIncome)}."
                } else {
                    "✅ Bạn đang kiểm soát chi tiêu rất tốt! Không có danh mục nào bị vỡ trận ngân sách và tỷ lệ tiết kiệm đạt ${String.format("%.1f", snapshot.savingsRate)}%."
                }
            }

            q.contains("tài sản ròng") || q.contains("có bao nhiêu tiền") || q.contains("tổng tài sản") -> {
                "💎 **Báo cáo Tài sản ròng (Net Worth):**\n" +
                "• Tổng tài sản có: ${formatVnd(snapshot.totalAssets)}\n" +
                "• Tổng nợ & Thẻ tín dụng: ${formatVnd(snapshot.totalLiabilities)}\n" +
                "━━━━━━━━━━━━━━━━━\n" +
                "👉 **Tài sản ròng thực tế: ${formatVnd(snapshot.netWorth)}**"
            }

            q.contains("ô tô") || q.contains("mua xe") || q.contains("mua nhà") -> {
                val target = if (q.contains("800")) 800000000.0 else 500000000.0
                val monthlySave = if (snapshot.monthlySavings > 0) snapshot.monthlySavings else 15000000.0
                val monthsNeeded = kotlin.math.ceil((target - snapshot.netWorth.coerceAtLeast(0.0)) / monthlySave).toInt().coerceAtLeast(1)
                val years = monthsNeeded / 12
                val remainingMonths = monthsNeeded % 12

                "🚗 **Phân tích kế hoạch mua tài sản (${formatVnd(target)}):**\n\n" +
                "• Tài sản ròng khả dụng: ${formatVnd(snapshot.netWorth.coerceAtLeast(0.0))}\n" +
                "• Số tiền còn thiếu: ${formatVnd((target - snapshot.netWorth).coerceAtLeast(0.0))}\n" +
                "• Tốc độ tích lũy trung bình: ~${formatVnd(monthlySave)}/tháng\n\n" +
                "⏱️ **Dự kiến:** Với tốc độ hiện tại, bạn cần khoảng **$monthsNeeded tháng** (${if (years > 0) "$years năm " else ""}$remainingMonths tháng) để hoàn thành mục tiêu này mà không cần vay mượn!"
            }

            else -> {
                "💡 **Tổng quan tài chính Thành Phát:**\n" +
                "Tháng này bạn đã thu **${formatVnd(snapshot.monthlyIncome)}**, chi **${formatVnd(snapshot.monthlyExpense)}**, tích lũy được **${formatVnd(snapshot.monthlySavings)}** (${String.format("%.1f", snapshot.savingsRate)}%).\n" +
                "Tài sản ròng hiện tại là **${formatVnd(snapshot.netWorth)}**.\n\n" +
                "Bạn có thể hỏi tôi về: 'Khoản chi nhiều nhất', 'Tháng này có tiêu quá tay không', hoặc 'Kế hoạch mua ô tô 800 triệu' nhé!"
            }
        }
    }
}
