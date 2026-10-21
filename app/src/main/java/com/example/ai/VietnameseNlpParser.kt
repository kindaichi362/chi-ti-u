package com.example.ai

import com.example.data.model.Account
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.TransactionType
import java.util.Locale
import java.util.regex.Pattern

data class ParsedTransaction(
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long?,
    val categoryName: String?,
    val sourceAccountId: Long,
    val sourceAccountName: String,
    val destinationAccountId: Long? = null,
    val destinationAccountName: String? = null,
    val note: String,
    val confidenceScore: Float
)

object VietnameseNlpParser {

    fun parse(
        input: String,
        availableAccounts: List<Account>,
        availableCategories: List<Category>
    ): ParsedTransaction? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Extract Amount
        val amount = extractAmount(lower) ?: return null

        // 2. Determine Transaction Type
        val isTransfer = lower.contains("chuyển") || lower.contains("rút tiền") ||
                lower.contains("bắn sang") || lower.contains("sang ví") || (lower.contains("sang") && !lower.contains("sáng"))
        val isIncome = !isTransfer && (
                lower.contains("lương") || lower.contains("thưởng") ||
                lower.contains("trả tôi") || lower.contains("trả nợ") ||
                lower.contains("nhận") || lower.contains("thu") ||
                lower.contains("được cho") || lower.contains("bán") ||
                lower.contains("lãi")
        )
        val transactionType = when {
            isTransfer -> TransactionType.TRANSFER
            isIncome -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }

        // 3. Match Accounts
        val matchedAccounts = matchAccounts(lower, availableAccounts)
        val defaultAccount = availableAccounts.firstOrNull() ?: Account(id = 1, name = "Tiền mặt", type = com.example.data.model.AccountType.CASH, balance = 0.0)

        val sourceAccount: Account
        var destAccount: Account? = null

        if (isTransfer) {
            if (matchedAccounts.size >= 2) {
                sourceAccount = matchedAccounts[0]
                destAccount = matchedAccounts[1]
            } else if (matchedAccounts.size == 1) {
                sourceAccount = matchedAccounts[0]
                destAccount = availableAccounts.firstOrNull { it.id != sourceAccount.id }
            } else {
                sourceAccount = defaultAccount
                destAccount = availableAccounts.getOrNull(1)
            }
        } else {
            sourceAccount = matchedAccounts.firstOrNull() ?: defaultAccount
        }

        // 4. Match Category
        val matchedCategory = matchCategory(lower, transactionType, availableCategories)

        // 5. Clean Note
        val note = cleanNote(trimmed)

        return ParsedTransaction(
            type = transactionType,
            amount = amount,
            categoryId = matchedCategory?.id,
            categoryName = matchedCategory?.name,
            sourceAccountId = sourceAccount.id,
            sourceAccountName = sourceAccount.name,
            destinationAccountId = destAccount?.id,
            destinationAccountName = destAccount?.name,
            note = note,
            confidenceScore = 0.95f
        )
    }

    private fun extractAmount(text: String): Double? {
        // Pattern 1: e.g. "350k", "45.5k", "50 nghìn", "70 ngàn"
        val kRegex = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:k|nghìn|ngàn)", Pattern.CASE_INSENSITIVE)
        val kMatcher = kRegex.matcher(text)
        if (kMatcher.find()) {
            val numStr = kMatcher.group(1)?.replace(',', '.') ?: "0"
            return numStr.toDoubleOrNull()?.times(1000.0)
        }

        // Pattern 2: e.g. "30 triệu", "2.5tr", "5 củ"
        val trRegex = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:tr|triệu|củ)", Pattern.CASE_INSENSITIVE)
        val trMatcher = trRegex.matcher(text)
        if (trMatcher.find()) {
            val numStr = trMatcher.group(1)?.replace(',', '.') ?: "0"
            return numStr.toDoubleOrNull()?.times(1000000.0)
        }

        // Pattern 3: e.g. "5 lít" (= 500k in VN slang)
        val litRegex = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:lít)", Pattern.CASE_INSENSITIVE)
        val litMatcher = litRegex.matcher(text)
        if (litMatcher.find()) {
            val numStr = litMatcher.group(1)?.replace(',', '.') ?: "0"
            return numStr.toDoubleOrNull()?.times(100000.0)
        }

        // Pattern 4: Full formatted number e.g. "350.000", "1.500.000" or raw "350000"
        val numRegex = Pattern.compile("(?:^|\\s)([0-9]{1,3}(?:\\.[0-9]{3})+|[0-9]{4,10})(?:\\s*₫|\\s*đ)?(?:\\s|$)", Pattern.CASE_INSENSITIVE)
        val numMatcher = numRegex.matcher(text)
        if (numMatcher.find()) {
            val raw = numMatcher.group(1)?.replace(".", "") ?: "0"
            return raw.toDoubleOrNull()
        }

        // Pattern 5: Simple 2-3 digit fallback if followed by nothing (e.g. "Ăn trưa 40")
        val simpleRegex = Pattern.compile("(\\d+)(?:\\s*\$)", Pattern.CASE_INSENSITIVE)
        val simpleMatcher = simpleRegex.matcher(text)
        if (simpleMatcher.find()) {
            val raw = simpleMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            return if (raw < 1000) raw * 1000.0 else raw
        }

        return null
    }

    private fun matchAccounts(text: String, accounts: List<Account>): List<Account> {
        val matched = mutableListOf<Account>()
        val lower = text.lowercase(Locale.ROOT)

        for (acc in accounts) {
            val nameLower = acc.name.lowercase(Locale.ROOT)
            val codeLower = acc.bankCode?.lowercase(Locale.ROOT) ?: ""

            val hits = (codeLower.isNotEmpty() && lower.contains(codeLower)) ||
                    (codeLower == "vcb" && (lower.contains("vietcom") || lower.contains("vcb"))) ||
                    (codeLower == "tcb" && (lower.contains("techcom") || lower.contains("tech") || lower.contains("tcb"))) ||
                    (codeLower == "mbbank" && (lower.contains("mb") || lower.contains("mbbank"))) ||
                    (codeLower == "momo" && lower.contains("momo")) ||
                    (acc.type == com.example.data.model.AccountType.CASH && (lower.contains("tiền mặt") || lower.contains("ví"))) ||
                    (acc.type == com.example.data.model.AccountType.CREDIT_CARD && (lower.contains("thẻ tín dụng") || lower.contains("credit") || lower.contains("vpbank"))) ||
                    lower.contains(nameLower)

            if (hits && !matched.contains(acc)) {
                matched.add(acc)
            }
        }
        return matched
    }

    private fun unaccent(text: String): String {
        val temp = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(temp).replaceAll("")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .lowercase(Locale.ROOT)
    }

    private fun matchCategory(
        text: String,
        type: TransactionType,
        categories: List<Category>
    ): Category? {
        val targetCategories = categories.filter {
            if (type == TransactionType.INCOME) it.type == CategoryType.INCOME else it.type == CategoryType.EXPENSE
        }
        val lower = text.lowercase(Locale.ROOT)
        val unaccented = unaccent(text)

        // 0. Direct match against specific category names (e.g. "Ăn sáng", "Quần áo", "Cà phê", "Trà sữa", "Tiền điện")
        val sortedTargets = targetCategories.sortedByDescending { it.name.length }
        for (cat in sortedTargets) {
            val catUnaccent = unaccent(cat.name).replace("/", " ").trim()
            val catLower = cat.name.lowercase(Locale.ROOT)
            if (catUnaccent.length >= 4 && (unaccented.contains(catUnaccent) || lower.contains(catLower))) {
                return cat
            }
        }

        // 1. FUEL / TRANSPORTATION KEYWORDS (Fixing "đỏ xăng", "đổ xăng", "xăng xe" - NEVER default to Ăn uống!)
        val fuelKeywords = listOf(
            "đỏ xăng", "đổ xăng", "đo xăng", "đỗ xăng", "đồ xăng", "đọ xăng", "tiền xăng", "xăng xe", "xăng",
            "bơm xăng", "nạp xăng", "mua xăng", "cây xăng", "petrolimex", "pvoil", "pv oil", "dầu nhớt", "thay nhớt",
            "thay dầu", "nhớt xe", "nhớt", "rửa xe", "gửi xe", "vé gửi xe", "sửa xe", "vá xe", "vá lốp", "thay lốp",
            "bảo dưỡng xe", "bảo dưỡng", "cứu hộ xe", "đăng kiểm", "phí bot", "cầu đường", "vé cầu đường", "trạm bot",
            "vetc", "epass",
            // Unaccented
            "do xang", "xang xe", "xang", "bom xang", "nap xang", "cay xang", "mua xang", "tien xang", "thay nhot",
            "nhot", "dau nhot", "rua xe", "gui xe", "sua xe", "va xe", "phi bot"
        )
        for (kw in fuelKeywords) {
            if (lower.contains(kw) || unaccented.contains(kw) || lower.split(" ").contains(kw) || unaccented.split(" ").contains(kw)) {
                val gasCat = targetCategories.firstOrNull {
                    val n = unaccent(it.name)
                    n.contains("xang")
                } ?: targetCategories.firstOrNull {
                    val n = unaccent(it.name)
                    n.contains("xe") || n.contains("di chuyen") || n.contains("di lai")
                } ?: targetCategories.firstOrNull { it.id == 2L }
                ?: Category(
                    id = 2L,
                    name = "Xăng xe",
                    type = CategoryType.EXPENSE,
                    iconName = "local_gas_station",
                    colorHex = "#F97316",
                    emoji = "⛽",
                    displayOrder = 2
                )
                return gasCat
            }
        }

        // Semantic keyword rules matching actual database category names
        val keywordsMap = mapOf(
            "Xăng xe" to listOf("đổ xăng", "đỏ xăng", "xăng", "petrolimex", "gửi xe", "sửa xe", "rửa xe", "thay nhớt", "do xang", "xang"),
            "Đi lại" to listOf("grab", "be", "taxi", "vé xe", "xe bus", "tàu hỏa", "máy bay", "xanh sm"),
            "Ăn uống" to listOf("ăn sáng", "ăn trưa", "ăn tối", "phở", "cơm", "bún", "bánh mì", "lẩu", "nướng", "hủ tiếu", "cà phê", "cafe", "trà sữa"),
            "Mua sắm" to listOf("quần áo", "giày", "dép", "shopee", "lazada", "tiki", "mua sắm", "mỹ phẩm", "son"),
            "Điện nước" to listOf("tiền điện", "tiền nước", "evn", "điện nước", "tiền wifi", "internet"),
            "Hóa đơn" to listOf("hóa đơn", "tiền nhà", "thuê nhà", "chung cư", "phí quản lý"),
            "Giải trí" to listOf("xem phim", "cinema", "cgv", "netflix", "spotify", "karaoke", "chơi game"),
            "Sức khỏe" to listOf("thuốc", "bệnh viện", "khám", "nha khoa", "bác sĩ"),
            "Giáo dục" to listOf("học phí", "sách", "khóa học", "tiền học"),
            "Lương" to listOf("lương", "nhận lương", "bắn lương", "luong"),
            "Thưởng" to listOf("thưởng", "bonus", "thuong")
        )

        for ((catName, keywords) in keywordsMap) {
            for (kw in keywords) {
                if (lower.contains(kw) || unaccented.contains(unaccent(kw))) {
                    val found = targetCategories.firstOrNull { unaccent(it.name).contains(unaccent(catName)) }
                    if (found != null) return found
                }
            }
        }

        // Do not default to Ăn uống when not matched
        return null
    }

    private fun cleanNote(text: String): String {
        return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}
