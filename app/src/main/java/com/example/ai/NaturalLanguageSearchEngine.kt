package com.example.ai

import com.example.data.model.Account
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.formatVnd
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class NaturalQueryResult(
    val query: String,
    val summaryText: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val filteredTransactions: List<Transaction>
)

object NaturalLanguageSearchEngine {

    fun executeQuery(
        query: String,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        categories: List<Category>
    ): NaturalQueryResult {
        val lower = query.lowercase(Locale.ROOT).trim()

        // 1. Determine Date Range Filter
        val (startDate, endDate, timeDesc) = resolveDateRange(lower)

        // 2. Determine Category Filter
        val matchedCategory = categories.firstOrNull { cat ->
            lower.contains(cat.name.lowercase(Locale.ROOT)) ||
            (cat.name.contains("Ăn uống", ignoreCase = true) && (lower.contains("ăn") || lower.contains("cơm") || lower.contains("phở"))) ||
            (cat.name.contains("Xăng xe", ignoreCase = true) && (lower.contains("xăng") || lower.contains("petrolimex"))) ||
            (cat.name.contains("Mua sắm", ignoreCase = true) && (lower.contains("mua sắm") || lower.contains("shopee"))) ||
            (cat.name.contains("Điện nước", ignoreCase = true) && (lower.contains("điện") || lower.contains("nước")))
        }

        // 3. Determine Amount Filter (e.g. "trên 1 triệu", "trên 500k", "dưới 200k")
        var minAmount: Double? = null
        var maxAmount: Double? = null

        val overRegex = Pattern.compile("(?:trên|hơn|lớn hơn|>)\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:k|nghìn|tr|triệu|củ|đ|₫)?", Pattern.CASE_INSENSITIVE)
        val overMatcher = overRegex.matcher(lower)
        if (overMatcher.find()) {
            val numStr = overMatcher.group(1)?.replace(',', '.') ?: "0"
            val multiplier = when {
                lower.contains("tr") || lower.contains("triệu") || lower.contains("củ") -> 1000000.0
                lower.contains("k") || lower.contains("nghìn") || lower.contains("ngàn") -> 1000.0
                else -> if ((numStr.toDoubleOrNull() ?: 0.0) < 1000) 1000000.0 else 1.0
            }
            minAmount = (numStr.toDoubleOrNull() ?: 0.0) * multiplier
        }

        val underRegex = Pattern.compile("(?:dưới|nhỏ hơn|<)\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:k|nghìn|tr|triệu|củ|đ|₫)?", Pattern.CASE_INSENSITIVE)
        val underMatcher = underRegex.matcher(lower)
        if (underMatcher.find()) {
            val numStr = underMatcher.group(1)?.replace(',', '.') ?: "0"
            val multiplier = when {
                lower.contains("tr") || lower.contains("triệu") || lower.contains("củ") -> 1000000.0
                lower.contains("k") || lower.contains("nghìn") || lower.contains("ngàn") -> 1000.0
                else -> if ((numStr.toDoubleOrNull() ?: 0.0) < 1000) 1000000.0 else 1.0
            }
            maxAmount = (numStr.toDoubleOrNull() ?: 0.0) * multiplier
        }

        // 4. Filter Transactions strictly
        val matchedList = allTransactions.filter { tx ->
            // Date filter
            val inDateRange = tx.date in startDate..endDate

            // Category filter
            val matchCat = matchedCategory == null || tx.categoryId == matchedCategory.id

            // Amount filter
            val matchMin = minAmount == null || tx.amount >= minAmount
            val matchMax = maxAmount == null || tx.amount <= maxAmount

            inDateRange && matchCat && matchMin && matchMax
        }

        val totalAmount = matchedList.sumOf {
            if (it.type == TransactionType.EXPENSE) it.amount else if (it.type == TransactionType.INCOME) it.amount else 0.0
        }

        // 5. Construct Analytical Vietnamese Summary
        val summary = when {
            matchedList.isEmpty() -> {
                "Không tìm thấy giao dịch nào phù hợp với câu hỏi trong $timeDesc."
            }
            matchedCategory != null -> {
                "Trong $timeDesc, bạn có ${matchedList.size} giao dịch ${matchedCategory.name} với tổng số tiền ${formatVnd(totalAmount)}."
            }
            minAmount != null -> {
                "Có ${matchedList.size} khoản chi tiêu trên ${formatVnd(minAmount)} trong $timeDesc, tổng cộng ${formatVnd(totalAmount)}."
            }
            else -> {
                "Tìm thấy ${matchedList.size} giao dịch trong $timeDesc với tổng số tiền ${formatVnd(totalAmount)}."
            }
        }

        return NaturalQueryResult(
            query = query,
            summaryText = summary,
            totalAmount = totalAmount,
            transactionCount = matchedList.size,
            filteredTransactions = matchedList.sortedByDescending { it.date }
        )
    }

    private fun resolveDateRange(lower: String): Triple<Long, Long, String> {
        val cal = Calendar.getInstance()
        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return when {
            lower.contains("tháng trước") -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, maxDay)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                val monthName = "tháng " + (cal.get(Calendar.MONTH) + 1)
                Triple(start, end, "tháng trước ($monthName)")
            }
            lower.contains("hôm nay") -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                Triple(cal.timeInMillis, endOfToday, "hôm nay")
            }
            lower.contains("hôm qua") -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                val end = cal.timeInMillis
                Triple(start, end, "hôm qua")
            }
            lower.contains("ngày ") -> {
                // e.g. "Ngày 20 tôi tiêu gì?"
                val dayRegex = Pattern.compile("ngày\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE)
                val matcher = dayRegex.matcher(lower)
                if (matcher.find()) {
                    val day = matcher.group(1)?.toIntOrNull() ?: 1
                    cal.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    val start = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    val end = cal.timeInMillis
                    Triple(start, end, "ngày $day")
                } else {
                    getThisMonthRange(cal, endOfToday)
                }
            }
            else -> {
                // Default: "tháng này"
                getThisMonthRange(cal, endOfToday)
            }
        }
    }

    private fun getThisMonthRange(cal: Calendar, endOfToday: Long): Triple<Long, Long, String> {
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis
        val currentMonth = cal.get(Calendar.MONTH) + 1
        return Triple(start, endOfToday, "tháng này (tháng $currentMonth)")
    }
}
