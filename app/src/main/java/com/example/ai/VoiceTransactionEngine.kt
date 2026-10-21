package com.example.ai

import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.TransactionType
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class VoiceTransactionDraft(
    val type: TransactionType,
    val amount: Double?,
    val currency: String = "VND",
    val categoryId: Long?,
    val categoryName: String?,
    val categoryEmoji: String?,
    val accountId: Long?,
    val accountName: String?,
    val bankId: String?,
    val destinationAccountId: Long? = null,
    val destinationAccountName: String? = null,
    val date: String,
    val note: String,
    val confidence: Float,
    val requiresConfirmation: Boolean,
    val clarificationQuestion: String? = null,
    val hasAmount: Boolean = false,
    val hasCategory: Boolean = false,
    val hasAccount: Boolean = false
) {
    val isComplete: Boolean
        get() = hasAmount && hasCategory && hasAccount && (amount != null && amount > 0) && (categoryId != null) && (accountId != null)

    fun toJsonString(): String {
        return """
        {
          "type": "${type.name.lowercase()}",
          "amount": ${amount?.toLong() ?: 0},
          "currency": "$currency",
          "category_id": ${categoryId ?: "null"},
          "category_name": "${categoryName ?: ""}",
          "account_id": ${accountId ?: "null"},
          "account_name": "${accountName ?: ""}",
          "date": "$date",
          "note": "$note",
          "confidence": $confidence,
          "has_amount": $hasAmount,
          "has_category": $hasCategory,
          "has_account": $hasAccount,
          "is_complete": $isComplete
        }
        """.trimIndent()
    }
}

object VoiceTransactionEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun unaccent(text: String): String {
        val temp = Normalizer.normalize(text, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(temp).replaceAll("")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .lowercase(Locale.ROOT)
    }

    fun processVoiceCommand(
        input: String,
        currentDraft: VoiceTransactionDraft?,
        availableAccounts: List<Account>,
        availableCategories: List<Category>
    ): VoiceTransactionDraft? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // If existing draft is being refined
        if (currentDraft != null && isRefinementCommand(trimmed)) {
            return refineDraft(currentDraft, trimmed, availableAccounts, availableCategories)
        }

        return parseNewTransaction(trimmed, availableAccounts, availableCategories, currentDraft)
    }

    private fun isRefinementCommand(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val unaccented = unaccent(text)
        return lower.startsWith("đổi") || lower.startsWith("thay") ||
                lower.startsWith("không phải") || lower.startsWith("sửa") ||
                lower.startsWith("chuyển sang") || lower.startsWith("thành") ||
                unaccented.startsWith("doi") || unaccented.startsWith("thay") ||
                unaccented.startsWith("khong phai") || unaccented.startsWith("sua") ||
                lower.contains("không phải") || unaccented.contains("khong phai")
    }

    private fun refineDraft(
        current: VoiceTransactionDraft,
        command: String,
        accounts: List<Account>,
        categories: List<Category>
    ): VoiceTransactionDraft {
        val lower = command.lowercase(Locale.ROOT)
        var updatedAmount = current.amount
        var updatedHasAmount = current.hasAmount
        var updatedAccountId = current.accountId
        var updatedAccountName = current.accountName
        var updatedBankId = current.bankId
        var updatedHasAccount = current.hasAccount
        var updatedCategoryId = current.categoryId
        var updatedCategoryName = current.categoryName
        var updatedCategoryEmoji = current.categoryEmoji
        var updatedHasCategory = current.hasCategory
        var updatedNote = current.note

        // 1. Refine Amount
        val parsedAmt = extractAmount(lower)
        if (parsedAmt != null && parsedAmt > 0) {
            updatedAmount = parsedAmt
            updatedHasAmount = true
        }

        // 2. Refine Account
        val matchedAcc = matchAccount(lower, accounts)
        if (matchedAcc != null) {
            updatedAccountId = matchedAcc.id
            updatedAccountName = matchedAcc.name
            updatedBankId = matchedAcc.bankId
            updatedHasAccount = true
        }

        // 3. Refine Category
        val matchedCat = matchCategory(lower, current.type, categories)
        if (matchedCat != null) {
            updatedCategoryId = matchedCat.id
            updatedCategoryName = matchedCat.name
            updatedCategoryEmoji = matchedCat.emoji
            updatedHasCategory = true
        }

        // 4. Refine Note
        if (lower.contains("ghi chú là") || lower.contains("ghi là")) {
            val notePart = command.substringAfter("ghi chú là").substringAfter("ghi là").trim()
            if (notePart.isNotBlank()) updatedNote = notePart
        }

        return current.copy(
            amount = updatedAmount,
            hasAmount = updatedHasAmount,
            accountId = updatedAccountId,
            accountName = updatedAccountName,
            bankId = updatedBankId,
            hasAccount = updatedHasAccount,
            categoryId = updatedCategoryId,
            categoryName = updatedCategoryName,
            categoryEmoji = updatedCategoryEmoji,
            hasCategory = updatedHasCategory,
            note = updatedNote,
            confidence = 0.98f,
            requiresConfirmation = true,
            clarificationQuestion = null
        )
    }

    private fun parseNewTransaction(
        text: String,
        accounts: List<Account>,
        categories: List<Category>,
        previousDraft: VoiceTransactionDraft? = null
    ): VoiceTransactionDraft {
        val lower = text.lowercase(Locale.ROOT)
        val unaccented = unaccent(text)
        val todayStr = dateFormat.format(Date())

        // 1. Amount Extraction (Element 1/3)
        val extractedAmount = extractAmount(lower)
        val amount = extractedAmount ?: previousDraft?.amount
        val hasAmount = extractedAmount != null || (previousDraft?.hasAmount == true && amount != null && amount > 0)

        // 2. Transaction Type Detection
        val isTransfer = lower.contains("chuyển") || lower.contains("rút tiền") ||
                lower.contains("bắn sang") || lower.contains("sang ví") ||
                unaccented.contains("chuyen") || unaccented.contains("rut tien") ||
                (lower.contains("sang") && !lower.contains("sáng") && !unaccented.contains("xang"))
        val isIncome = !isTransfer && (
                lower.contains("lương") || lower.contains("thưởng") ||
                lower.contains("trả tôi") || lower.contains("thu nợ") ||
                lower.contains("nhận") || lower.contains("thu vào") ||
                lower.contains("được cho") || lower.contains("bán") ||
                lower.contains("lãi") || unaccented.contains("luong") || unaccented.contains("thuong")
        )
        val type = when {
            isTransfer -> TransactionType.TRANSFER
            isIncome -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }

        // 3. Category Evaluation (Element 2/3) - Priority for Fuel/Transportation
        val matchedCategoryResult = evaluateCategoryDetailed(lower, unaccented, type, categories)
        val categoryId = matchedCategoryResult?.first?.id ?: previousDraft?.categoryId
        val categoryName = matchedCategoryResult?.first?.name ?: previousDraft?.categoryName
        val categoryEmoji = matchedCategoryResult?.first?.emoji ?: previousDraft?.categoryEmoji
        val hasCategory = matchedCategoryResult != null || (previousDraft?.hasCategory == true && categoryId != null)
        val confidence = matchedCategoryResult?.second ?: if (hasCategory) 0.90f else 0.50f

        // 4. Account Evaluation (Element 3/3)
        val matchedAccount = matchAccount(lower, accounts)
        val accountId = matchedAccount?.id ?: previousDraft?.accountId
        val accountName = matchedAccount?.name ?: previousDraft?.accountName
        val bankId = matchedAccount?.bankId ?: previousDraft?.bankId
        val hasAccount = matchedAccount != null || (previousDraft?.hasAccount == true && accountId != null)

        val note = cleanVoiceNote(text, amount ?: 0.0, accountName ?: "", categoryName)

        return VoiceTransactionDraft(
            type = type,
            amount = amount,
            currency = "VND",
            categoryId = categoryId,
            categoryName = categoryName,
            categoryEmoji = categoryEmoji ?: if (categoryId != null) "🏷️" else null,
            accountId = accountId,
            accountName = accountName,
            bankId = bankId,
            destinationAccountId = null,
            destinationAccountName = null,
            date = todayStr,
            note = note.ifBlank { categoryName ?: "Chi tiêu" },
            confidence = confidence,
            requiresConfirmation = true,
            hasAmount = hasAmount,
            hasCategory = hasCategory,
            hasAccount = hasAccount
        )
    }

    private fun extractAmount(text: String): Double? {
        val lower = text.lowercase(Locale.ROOT)
        val unaccented = unaccent(text)

        // 1. e.g. "50 nghìn", "50 ngàn", "50k", "50.5k", "50 nghin", "50 ngan"
        val kRegex = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:k|nghìn|ngàn|nghin|ngan)", Pattern.CASE_INSENSITIVE)
        val kMatcher = kRegex.matcher(lower)
        if (kMatcher.find()) {
            val numStr = kMatcher.group(1)?.replace(',', '.') ?: "0"
            return numStr.toDoubleOrNull()?.times(1000.0)
        }

        // 2. e.g. "30 triệu", "5 tr", "2 củ", "2 cu", "30 trieu"
        val trRegex = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:tr|triệu|trieu|củ|cu)", Pattern.CASE_INSENSITIVE)
        val trMatcher = trRegex.matcher(lower)
        if (trMatcher.find()) {
            val numStr = trMatcher.group(1)?.replace(',', '.') ?: "0"
            return numStr.toDoubleOrNull()?.times(1000000.0)
        }

        // 3. e.g. "5 lít", "5 lit" (= 500k)
        val litRegex = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:lít|lit)", Pattern.CASE_INSENSITIVE)
        val litMatcher = litRegex.matcher(lower)
        if (litMatcher.find()) {
            val numStr = litMatcher.group(1)?.replace(',', '.') ?: "0"
            return numStr.toDoubleOrNull()?.times(100000.0)
        }

        // 4. Formatted number e.g. 50.000 or 50000
        val numRegex = Pattern.compile("(?:^|\\s)([0-9]{1,3}(?:\\.[0-9]{3})+|[0-9]{4,10})(?:\\s*₫|\\s*đ|\\s*d)?(?:\\s|$)", Pattern.CASE_INSENSITIVE)
        val numMatcher = numRegex.matcher(lower)
        if (numMatcher.find()) {
            val raw = numMatcher.group(1)?.replace(".", "") ?: "0"
            return raw.toDoubleOrNull()
        }

        // 5. Standalone number e.g. "Ăn sáng 35", "Đổ xăng 70"
        val standaloneRegex = Pattern.compile("\\b(\\d{2,3})\\b")
        val standaloneMatcher = standaloneRegex.matcher(lower)
        if (standaloneMatcher.find()) {
            val num = standaloneMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            if (num in 10.0..999.0) {
                return num * 1000.0
            }
        }

        return null
    }

    private fun matchAccount(text: String, accounts: List<Account>): Account? {
        val lower = text.lowercase(Locale.ROOT)
        val unaccented = unaccent(text)

        for (acc in accounts) {
            val nameLower = acc.name.lowercase(Locale.ROOT)
            val nameUnaccented = unaccent(acc.name)
            val codeLower = acc.bankCode?.lowercase(Locale.ROOT) ?: ""
            val bankIdLower = acc.bankId?.lowercase(Locale.ROOT) ?: ""

            val isCash = acc.type == AccountType.CASH && (
                    lower.contains("tiền mặt") || unaccented.contains("tien mat") ||
                    lower.contains("ví tiền") || unaccented.contains("vi tien") ||
                    lower.contains("trong ví") || unaccented.contains("trong vi") ||
                    lower.contains("ví") || unaccented.contains("vi")
            )

            val isVcb = (codeLower == "vcb" || bankIdLower == "vcb") && (
                    lower.contains("vcb") || lower.contains("vietcom") || unaccented.contains("vietcom")
            )

            val isTcb = (codeLower == "tcb" || bankIdLower == "tcb") && (
                    lower.contains("tcb") || lower.contains("techcom") || unaccented.contains("techcom") || lower.contains("tech")
            )

            val isMb = (codeLower == "mb" || bankIdLower == "mb" || codeLower == "mbbank") && (
                    lower.contains("mb bank") || lower.contains("mbbank") || unaccented.contains("mb bank") ||
                    (lower.contains("mb") && !lower.contains("mua"))
            )

            val isMomo = (codeLower == "momo" || bankIdLower == "momo") && lower.contains("momo")

            val isBidv = (codeLower == "bidv" || bankIdLower == "bidv") && lower.contains("bidv")
            val isAcb = (codeLower == "acb" || bankIdLower == "acb") && lower.contains("acb")
            val isVp = (codeLower == "vpbank" || bankIdLower == "vpb") && (lower.contains("vp") || lower.contains("cake"))

            val nameMatch = lower.contains(nameLower) || unaccented.contains(nameUnaccented)

            if (isCash || isVcb || isTcb || isMb || isMomo || isBidv || isAcb || isVp || nameMatch) {
                return acc
            }
        }
        return null
    }

    private fun evaluateCategoryDetailed(
        lower: String,
        unaccented: String,
        type: TransactionType,
        categories: List<Category>
    ): Pair<Category, Float>? {
        val targetCategories = categories.filter {
            if (type == TransactionType.INCOME) it.type == CategoryType.INCOME else it.type == CategoryType.EXPENSE
        }

        // 0. Direct match against specific category names (Prioritize longest match e.g. "Ăn sáng", "Quần áo", "Cà phê", "Trà sữa")
        val sortedTargets = targetCategories.sortedByDescending { it.name.length }
        for (cat in sortedTargets) {
            val catUnaccent = unaccent(cat.name).replace("/", " ").trim()
            val catLower = cat.name.lowercase(Locale.ROOT)
            val parts = catUnaccent.split(" ").filter { it.length > 2 }
            if (catUnaccent.length >= 4 && (unaccented.contains(catUnaccent) || lower.contains(catLower))) {
                return Pair(cat, 0.99f)
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
                // Find "Xăng xe" with priority
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
                return Pair(gasCat, 0.99f)
            }
        }

        // 2. RIDE-HAILING / TAXI / TRAVEL
        val travelKeywords = listOf(
            "grab", "be", "gojek", "xanh sm", "taxi", "mai linh", "vinasun", "vé xe", "xe bus", "xe buýt",
            "vé tàu", "tàu hỏa", "máy bay", "vé máy bay", "vietjet", "vietnam airlines", "xe ôm",
            "ve xe", "may bay", "xe om"
        )
        for (kw in travelKeywords) {
            if (lower.contains(kw) || unaccented.contains(kw)) {
                val travelCat = targetCategories.firstOrNull {
                    val n = unaccent(it.name)
                    n.contains("di lai") || n.contains("du lich") || n.contains("xang")
                }
                if (travelCat != null) return Pair(travelCat, 0.98f)
            }
        }

        // 3. FOOD & DINING KEYWORDS
        val foodKeywords = listOf(
            "ăn sáng", "ăn trưa", "ăn tối", "ăn", "phở", "cơm", "bún", "bánh mì", "lẩu", "nướng",
            "hủ tiếu", "gỏi cuốn", "mì tôm", "cháo", "bánh cuốn", "nem", "ốc", "bánh xèo", "hải sản",
            "cà phê", "cafe", "coffee", "trà sữa", "nước ép", "sinh tố", "highlands", "starbucks", "phúc long",
            "an sang", "an trua", "an toi", "an", "pho", "com", "bun", "banh mi", "lau", "nuong", "ca phe", "tra sua"
        )
        for (kw in foodKeywords) {
            if (lower.contains(kw) || unaccented.contains(kw)) {
                val foodCat = targetCategories.firstOrNull {
                    val n = unaccent(it.name)
                    n.contains("an uong") || n.contains("an")
                }
                if (foodCat != null) return Pair(foodCat, 0.97f)
            }
        }

        // 4. SHOPPING
        val shopKeywords = listOf(
            "mua sắm", "quần áo", "giày", "dép", "shopee", "lazada", "tiki", "mỹ phẩm", "son",
            "mua do", "quan ao", "giay", "dep", "mua sam", "my pham"
        )
        for (kw in shopKeywords) {
            if (lower.contains(kw) || unaccented.contains(kw)) {
                val shopCat = targetCategories.firstOrNull { unaccent(it.name).contains("mua sam") }
                if (shopCat != null) return Pair(shopCat, 0.97f)
            }
        }

        // 5. BILLS & UTILITIES
        val billKeywords = listOf(
            "tiền điện", "tiền nước", "evn", "điện nước", "tiền wifi", "internet", "cước", "nạp thẻ", "tiền rác",
            "tien dien", "tien nuoc", "dien nuoc", "tien wifi"
        )
        for (kw in billKeywords) {
            if (lower.contains(kw) || unaccented.contains(kw)) {
                val billCat = targetCategories.firstOrNull {
                    val n = unaccent(it.name)
                    n.contains("dien nuoc") || n.contains("hoa don") || n.contains("dien thoai")
                }
                if (billCat != null) return Pair(billCat, 0.97f)
            }
        }

        // 6. HEALTHCARE
        val healthKeywords = listOf("thuốc", "bệnh viện", "khám bệnh", "nha khoa", "bác sĩ", "mua thuoc", "kham benh", "benh vien")
        for (kw in healthKeywords) {
            if (lower.contains(kw) || unaccented.contains(kw)) {
                val healthCat = targetCategories.firstOrNull { unaccent(it.name).contains("suc khoe") }
                if (healthCat != null) return Pair(healthCat, 0.97f)
            }
        }

        // 7. ENTERTAINMENT
        val entKeywords = listOf("xem phim", "cinema", "cgv", "netflix", "spotify", "karaoke", "chơi game", "xem phim", "choi game")
        for (kw in entKeywords) {
            if (lower.contains(kw) || unaccented.contains(kw)) {
                val entCat = targetCategories.firstOrNull { unaccent(it.name).contains("giai tri") }
                if (entCat != null) return Pair(entCat, 0.97f)
            }
        }

        // 8. SALARY & INCOME
        val incomeKeywords = listOf("lương", "nhận lương", "bắn lương", "lương tháng", "thưởng", "hoa hồng", "luong", "thuong")
        for (kw in incomeKeywords) {
            if (lower.contains(kw) || unaccented.contains(kw)) {
                val incCat = targetCategories.firstOrNull {
                    val n = unaccent(it.name)
                    n.contains("luong") || n.contains("thuong") || n.contains("thu nhap")
                }
                if (incCat != null) return Pair(incCat, 0.98f)
            }
        }

        // If no keyword match, DO NOT guess or default to food! Return null so user sees picker!
        return null
    }

    private fun matchCategory(
        text: String,
        type: TransactionType,
        categories: List<Category>
    ): Category? {
        val lower = text.lowercase(Locale.ROOT)
        val unaccented = unaccent(text)
        return evaluateCategoryDetailed(lower, unaccented, type, categories)?.first
    }

    private fun cleanVoiceNote(
        rawText: String,
        amount: Double,
        accountName: String,
        categoryName: String?
    ): String {
        var cleaned = rawText
        val phrasesToRemove = listOf(
            "hết", "bằng", "qua", "từ", "vào", "tiền mặt", "bằng tiền mặt",
            "mb bank", "vietcombank", "techcombank", "momo", "vcb", "tcb"
        )
        for (p in phrasesToRemove) {
            cleaned = cleaned.replace(p, "", ignoreCase = true)
        }
        cleaned = cleaned.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        return cleaned.ifBlank { categoryName ?: "Chi tiêu" }
    }
}
